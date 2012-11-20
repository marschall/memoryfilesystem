package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.channels.FileLockInterruptionException;
import java.nio.channels.OverlappingFileLockException;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final class LockSet {

  private final NavigableMap<Long, FileLock> locks;

  private final Lock lock;

  private final Condition removed;

  LockSet() {
    this.locks = new TreeMap<>();
    this.lock = new ReentrantLock();
    this.removed = this.lock.newCondition();
  }

  FileLock tryLock(FileLock lock) {
    try (AutoRelease autoRelease = AutoReleaseLock.autoRelease(this.lock)) {
      return this.tryPut(lock);
    }
  }

  private FileLock tryPut(FileLock lock) {
    Entry<Long, FileLock> ceilingEntry = this.locks.ceilingEntry(lock.position());
    Entry<Long, FileLock> floorEntry = this.locks.floorEntry(lock.position());

    if (ceilingEntry != null && ceilingEntry.getValue().overlaps(lock.position(), lock.size())) {
      return null;
    }

    if (floorEntry != null && floorEntry.getValue().overlaps(lock.position(), lock.size())) {
      return null;
    }

    this.locks.put(lock.position(), lock);

    return lock;
  }

  FileLock lock(FileLock lock) throws IOException {
    try (AutoRelease autoRelease = AutoReleaseLock.autoRelease(this.lock)) {
      if (Thread.currentThread().isInterrupted()) {
        throw new FileLockInterruptionException();
      }
      FileLock returnValue = this.tryLock(lock);
      if (returnValue == null) {
        throw new OverlappingFileLockException();
      }
      return returnValue;
    }
  }

  void remove(MemoryFileLock lock) {
    try (AutoRelease autoRelease = AutoReleaseLock.autoRelease(this.lock)) {
      // no need to check for return value because FileLock#release
      // can be invoked several times from multiple threads
      this.locks.remove(lock.position());
      lock.invalidate();
      this.removed.signalAll();
    }

  }

}

package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.channels.FileLockInterruptionException;
import java.nio.channels.OverlappingFileLockException;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

final class LockSet {

  private final NavigableMap<Long, MemoryFileLock> locks;

  LockSet() {
    this.locks = new TreeMap<>();
  }

  MemoryFileLock tryLock(MemoryFileLock lock) {
    return this.tryPut(lock);
  }

  private MemoryFileLock tryPut(MemoryFileLock lock) {
    Entry<Long, MemoryFileLock> ceilingEntry = this.locks.ceilingEntry(lock.position());
    Entry<Long, MemoryFileLock> floorEntry = this.locks.floorEntry(lock.position());

    if (ceilingEntry != null && ceilingEntry.getValue().overlaps(lock.position(), lock.size())) {
      return null;
    }

    if (floorEntry != null && floorEntry.getValue().overlaps(lock.position(), lock.size())) {
      return null;
    }

    this.locks.put(lock.position(), lock);

    return lock;
  }

  MemoryFileLock lock(MemoryFileLock lock) throws IOException {
    if (Thread.currentThread().isInterrupted()) {
      throw new FileLockInterruptionException();
    }
    MemoryFileLock returnValue = this.tryLock(lock);
    if (returnValue == null) {
      throw new OverlappingFileLockException();
    }
    return returnValue;
  }

  void remove(MemoryFileLock lock) {
    // no need to check for return value because FileLock#release
    // can be invoked several times from multiple threads
    this.locks.remove(lock.position());
    lock.invalidate();
  }

}

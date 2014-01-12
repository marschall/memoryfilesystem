package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.MemoryWatchKey.State.READY;
import static com.github.marschall.memoryfilesystem.MemoryWatchKey.State.SIGNALLED;

import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.Watchable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final class MemoryWatchKey implements WatchKey {

  private final AbstractPath path;
  private final Lock lock;
  private boolean isOverflow;
  private State state;
  private boolean valid;
  private List<WatchEvent<?>> accumulatedEvents;
  private Map<AbstractPath, Integer> accumulatedModificationEvents;
  private List<WatchEvent<?>> pendingEvents;

  MemoryWatchKey(AbstractPath path) {
    this.path = path;
    this.state = READY;
    this.lock = new ReentrantLock();
  }

  @Override
  public List<WatchEvent<?>> pollEvents() {
    try (AutoRelease autoRelease = AutoReleaseLock.autoRelease(this.lock)) {
      if (this.state != SIGNALLED) {
        // TODO throw exception?
      }

      if (this.pendingEvents == null) {
        return Collections.emptyList();
      }
      List<WatchEvent<?>> result = this.pendingEvents;
      this.pendingEvents = null;
      return result;
    }
  }

  @Override
  public boolean reset() {
    try (AutoRelease autoRelease = AutoReleaseLock.autoRelease(this.lock)) {
      if (!this.valid) {
        return false;
      }
      if (this.pendingEvents != null) {
        // TODO requeue
        this.state = READY;
      }

      return true;
    }
  }

  @Override
  public void cancel() {
    try (AutoRelease autoRelease = AutoReleaseLock.autoRelease(this.lock)) {
      this.valid = false;
      // TODO actually cancel registration
    }
  }

  @Override
  public boolean isValid() {
    try (AutoRelease autoRelease = AutoReleaseLock.autoRelease(this.lock)) {
      return this.valid;
    }
  }

  @Override
  public Watchable watchable() {
    return this.path;
  }

  enum State {
    READY,
    SIGNALLED;
  }

}

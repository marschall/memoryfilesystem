package com.github.marschall.memoryfilesystem;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

class ClosedChecker {

  static final AtomicIntegerFieldUpdater<ClosedChecker> OPEN_UPDATER =
          AtomicIntegerFieldUpdater.newUpdater(ClosedChecker.class, "open");

  private static final int OPEN = 1;
  private static final int CLOSED = 0;

  @SuppressWarnings("unused") // OPEN_UPDATER
  private volatile int open;

  ClosedChecker() {
    OPEN_UPDATER.set(this, OPEN);
  }

  boolean isOpen() {
    return OPEN_UPDATER.get(this) == OPEN;
  }

  boolean close() {
    return OPEN_UPDATER.getAndSet(this, CLOSED) == OPEN;
  }

}
package com.google.code.memoryfilesystem;

import java.util.concurrent.locks.Lock;


final class AutoReleaseLock implements AutoRelease {

  private final Lock lock;

  AutoReleaseLock(Lock lock) {
    this.lock = lock;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    this.lock.unlock();
  }

  static AutoRelease autoRelease(Lock lock) {
    lock.lock();
    return new AutoReleaseLock(lock);
  }

}
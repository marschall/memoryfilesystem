package com.github.marschall.memoryfilesystem;

import java.util.concurrent.locks.Lock;


final class AutoReleaseLock implements AutoRelease {

  private final Lock lock;

  AutoReleaseLock(Lock lock) {
    this.lock = lock;
  }


  @Override
  public void close() {
    this.lock.unlock();
  }

  static AutoRelease autoRelease(Lock lock) {
    lock.lock();
    return new AutoReleaseLock(lock);
  }

}
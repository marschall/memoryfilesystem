package com.github.marschall.memoryfilesystem;

import java.util.concurrent.atomic.AtomicBoolean;

class ClosedChecker {

  final AtomicBoolean open;

  ClosedChecker() {
    this.open = new AtomicBoolean(true);
  }

  boolean isOpen() {
    return this.open.get();
  }

  boolean close() {
    return this.open.getAndSet(false);
  }

}
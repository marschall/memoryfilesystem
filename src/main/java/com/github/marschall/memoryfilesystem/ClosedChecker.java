package com.github.marschall.memoryfilesystem;

class ClosedChecker {

  volatile boolean open;

  ClosedChecker() {
    this.open = true;
  }

  boolean isOpen() {
    return this.open;
  }

  void close() {
    this.open = false;
  }

}
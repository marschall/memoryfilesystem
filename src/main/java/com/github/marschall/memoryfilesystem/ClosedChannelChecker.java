package com.github.marschall.memoryfilesystem;

import java.nio.channels.ClosedChannelException;

final class ClosedChannelChecker {

  private volatile boolean open;


  ClosedChannelChecker() {
    this.open = true;
  }

  boolean isOpen() {
    return this.open;
  }

  void close() {
    this.open = false;
  }

  void check() throws ClosedChannelException {
    if (!open) {
      throw new ClosedChannelException();
    }
  }

}

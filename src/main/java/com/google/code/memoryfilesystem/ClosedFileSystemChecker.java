package com.google.code.memoryfilesystem;

import java.nio.file.ClosedFileSystemException;

final class ClosedFileSystemChecker {

  private volatile boolean open;


  ClosedFileSystemChecker() {
    this.open = true;
  }

  boolean isOpen() {
    return this.open;
  }

  void close() {
    this.open = false;
  }

  void check() {
    if (!open) {
      throw new ClosedFileSystemException();
    }
  }
}
package com.github.marschall.memoryfilesystem;

import java.nio.file.ClosedFileSystemException;

final class ClosedFileSystemChecker extends ClosedChecker {

  void check() {
    if (!open) {
      throw new ClosedFileSystemException();
    }
  }
}
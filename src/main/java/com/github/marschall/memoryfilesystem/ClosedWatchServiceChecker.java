package com.github.marschall.memoryfilesystem;

import java.nio.file.ClosedWatchServiceException;

final class ClosedWatchServiceChecker extends ClosedChecker {
  
  void check() {
    if (!open) {
      throw new ClosedWatchServiceException();
    }
  }

}

package com.github.marschall.memoryfilesystem;

import java.nio.file.AccessDeniedException;

final class OneTimePermissionChecker {

  private volatile boolean checkPassed;

  private final PermissionChecker delegate;

  OneTimePermissionChecker(PermissionChecker delegate) {
    this.delegate = delegate;
    this.checkPassed = false;
  }

  void checkPermission() throws AccessDeniedException {
    if (!this.checkPassed) {
      this.delegate.checkPermission();
      this.checkPassed = true;
    }
  }

  @FunctionalInterface
  interface PermissionChecker {

    void checkPermission() throws AccessDeniedException;

  }

}

package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.Path;

import com.github.marschall.memoryfilesystem.OneTimePermissionChecker.PermissionChecker;

class AppendingBlockOutputStream extends BlockOutputStream {

  AppendingBlockOutputStream(MemoryContents memoryContents, boolean deleteOnClose, Path path, PermissionChecker permissionChecker) {
    super(memoryContents, deleteOnClose, path, permissionChecker);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    this.checker.check(this.path);
    this.permissionChecker.checkPermission();
    this.memoryContents.writeAtEnd(b, off, len);
  }

}

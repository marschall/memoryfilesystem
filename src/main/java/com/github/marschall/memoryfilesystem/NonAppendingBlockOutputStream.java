package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import com.github.marschall.memoryfilesystem.OneTimePermissionChecker.PermissionChecker;

final class NonAppendingBlockOutputStream extends BlockOutputStream {

  static final AtomicLongFieldUpdater<NonAppendingBlockOutputStream> POSITION_UPDATER =
          AtomicLongFieldUpdater.newUpdater(NonAppendingBlockOutputStream.class, "position");

  @SuppressWarnings("unused") // POSITION_UPDATER
  private volatile long position;


  NonAppendingBlockOutputStream(MemoryContents memoryContents, boolean deleteOnClose, Path path, PermissionChecker permissionChecker) {
    super(memoryContents, deleteOnClose, path, permissionChecker);
    POSITION_UPDATER.set(this, 0L);
  }


  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    this.checker.check(this.path);
    this.permissionChecker.checkPermission();
    this.memoryContents.write(b, POSITION_UPDATER.get(this), off, len);
    POSITION_UPDATER.getAndAdd(this, len);
  }

}

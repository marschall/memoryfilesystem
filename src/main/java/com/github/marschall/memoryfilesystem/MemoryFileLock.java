package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.concurrent.atomic.AtomicBoolean;

final class MemoryFileLock extends FileLock {

  private final LockSet lockSet;

  private final AtomicBoolean valid;

  MemoryFileLock(FileChannel channel, long position, long size, boolean shared, LockSet lockSet) {
    super(channel, position, size, shared);
    this.lockSet = lockSet;
    this.valid = new AtomicBoolean(true);
  }

  MemoryFileLock(AsynchronousFileChannel channel, long position, long size, boolean shared, LockSet lockSet) {
    super(channel, position, size, shared);
    this.lockSet = lockSet;
    this.valid = new AtomicBoolean(true);
  }

  @Override
  public boolean isValid() {
    return this.valid.get();
  }

  @Override
  public void release() throws IOException {
    if (!this.channel().isOpen()) {
      throw new ClosedChannelException();
    }
    if (this.isValid()) {
      this.lockSet.remove(this);
    }

  }

  void invalidate() {
    this.valid.set(false);
  }

}

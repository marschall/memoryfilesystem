package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

final class MemoryFileLock extends FileLock {

  static final AtomicIntegerFieldUpdater<MemoryFileLock> VALID_UPDATER =
          AtomicIntegerFieldUpdater.newUpdater(MemoryFileLock.class, "valid");

  private static final int VALID = 1;
  private static final int INVALID = 0;

  @SuppressWarnings("unused") // VALID_UPDATER
  private volatile int valid;

  MemoryFileLock(FileChannel channel, long position, long size, boolean shared) {
    super(channel, position, size, shared);
    VALID_UPDATER.set(this, VALID);
  }

  MemoryFileLock(AsynchronousFileChannel channel, long position, long size, boolean shared) {
    super(channel, position, size, shared);
    VALID_UPDATER.set(this, VALID);
  }

  @Override
  public boolean isValid() {
    return VALID_UPDATER.get(this) == VALID;
  }

  @Override
  public void release() throws IOException {
    if (!this.acquiredBy().isOpen()) {
      throw new ClosedChannelException();
    }
    if (this.isValid()) {
      FileChannel channel = this.channel();
      if (channel instanceof BlockChannel) {
        BlockChannel blockChannel = (BlockChannel) channel;
        blockChannel.removeLock(this);
      } else {
        throw new AssertionError("unknown channel type: " + channel);
      }
    }

  }

  void invalidate() {
    VALID_UPDATER.set(this, INVALID);
  }

}

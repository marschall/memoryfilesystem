package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.concurrent.atomic.AtomicBoolean;

final class MemoryFileLock extends FileLock {

  private final AtomicBoolean valid;

  MemoryFileLock(FileChannel channel, long position, long size, boolean shared) {
    super(channel, position, size, shared);
    this.valid = new AtomicBoolean(true);
  }

  MemoryFileLock(AsynchronousFileChannel channel, long position, long size, boolean shared) {
    super(channel, position, size, shared);
    this.valid = new AtomicBoolean(true);
  }

  @Override
  public boolean isValid() {
    return this.valid.get();
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
    this.valid.set(false);
  }

}

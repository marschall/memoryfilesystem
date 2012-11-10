package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;

final class AppendingBlockChannel extends BlockChannel {

  private final long startPosition;

  AppendingBlockChannel(MemoryContents memoryContents, boolean readable, long startPosition) {
    super(memoryContents, readable, true);
    this.startPosition = startPosition;
  }

  @Override
  void writeCheck() throws ClosedChannelException {
    // an appending channel is always writable
    this.closedCheck();
  }

  //TODO override position(long)?

  @Override
  public SeekableByteChannel truncate(long size) throws IOException {
    // TODO use FileSystemException?
    throw new IOException("truncation not supported in append mode");
  }

  @Override
  public int write(ByteBuffer src) throws IOException {
    try (AutoRelease lock = this.writeLock()) {
      int written = this.memoryContents.writeAtEnd(src);
      this.position = this.memoryContents.size();
      return written;
    }
  }

}

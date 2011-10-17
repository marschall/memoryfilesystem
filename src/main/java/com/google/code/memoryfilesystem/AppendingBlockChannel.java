package com.google.code.memoryfilesystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

final class AppendingBlockChannel extends BlockChannel {

  private final long startPosition;

  AppendingBlockChannel(MemoryContents memoryContents, boolean readable, long startPosition) {
    super(memoryContents, readable, true);
    this.startPosition = startPosition;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  void writeCheck() {
    // an appending channel is always writable
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public SeekableByteChannel truncate(long size) throws IOException {
    throw new IOException("truncation not supported in append mode");
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public int write(ByteBuffer src) throws IOException {
    try (AutoRelease lock = writeLock()) {
      int written = this.memoryContents.writeAtEnd(src);
      this.position = this.memoryContents.size();
      return written;
    }
  }

}

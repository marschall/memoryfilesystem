package com.google.code.memoryfilesystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;

final class NonAppendingBlockChannel extends BlockChannel {

  private final boolean writable;



  NonAppendingBlockChannel(MemoryContents memoryContents, boolean readable, boolean writable) {
    super(memoryContents, readable, writable);
    this.writable = writable;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  void writeCheck() {
    if (!this.writable) {
      throw new NonWritableChannelException();
    }
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public int write(ByteBuffer src) throws IOException {
    try (AutoRelease lock = writeLock()) {
      int written = this.memoryContents.write(src, this.position);
      this.position += written;
      return written;
    }
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public SeekableByteChannel truncate(long size) throws IOException {
    //TODO if append advance
    try (AutoRelease lock = writeLock()) {
      this.memoryContents.truncate(size);
    }
    return this;
  }

}

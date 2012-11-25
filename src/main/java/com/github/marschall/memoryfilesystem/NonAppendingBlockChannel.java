package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;

final class NonAppendingBlockChannel extends BlockChannel {

  private final boolean writable;

  NonAppendingBlockChannel(MemoryContents memoryContents, boolean readable, boolean writable, boolean deleteOnClose, Path path) {
    super(memoryContents, readable, deleteOnClose, path);
    this.writable = writable;
  }


  @Override
  void writeCheck() throws ClosedChannelException {
    if (!this.writable) {
      throw new NonWritableChannelException();
    }
    this.closedCheck();
  }

  @Override
  public int write(ByteBuffer src, long position) throws IOException {
    try (AutoRelease lock = this.writeLock()) {
      return this.memoryContents.writeShort(src, position);
    }
  }


  @Override
  public int write(ByteBuffer src) throws IOException {
    try (AutoRelease lock = this.writeLock()) {
      int written = this.memoryContents.writeShort(src, this.position);
      this.position += written;
      return written;
    }
  }


  @Override
  public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
    this.validatePositionAndCount(position, count);
    try (AutoRelease lock = this.writeLock()) {
      return this.memoryContents.transferFrom(src, position, count);
    }
  }

  @Override
  public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
    this.validateOffsetAndLength(srcs, offset, length);
    try (AutoRelease lock = this.writeLock()) {
      // we have to take care this value does not overflow since a single buffer
      // can hold more than Integer.MAX_VALUE bytes and this method returns a long
      long totalWritten = 0L;
      for (int i = 0; i < length; ++i) {
        if (totalWritten == Long.MAX_VALUE) {
          break;
        }

        // we have to make sure a buffers capacity is exhausted before writing into the
        // next buffer so we use the method that returns a long
        long written = this.memoryContents.write(srcs[offset + 1], this.position, Long.MAX_VALUE - totalWritten);
        if (written != -1) {
          // we could read data, update position and total counter
          this.position += written;
          totalWritten += written;
        } else if (i == 0) {
          // we could not read and it was the first try a reading
          // (i.e. no previous attempt was made)
          return -1L;
        } else {
          // we could not read but previous attempts were successful
          // return result of previous attempts
          break;
        }
      }
      return totalWritten;
    }
  }

  @Override
  public FileChannel truncate(long size) throws IOException {
    try (AutoRelease lock = this.writeLock()) {
      this.memoryContents.truncate(size);
      // REVIEW, not sure from doc but kinda makes sense
      this.position = this.memoryContents.size();
    }
    return this;
  }

  @Override
  public void force(boolean metaData) throws IOException {
    if (metaData) {
      if (!this.writable) {
        this.memoryContents.accessed();
      } else {
        this.memoryContents.modified();
      }
    }
  }

}

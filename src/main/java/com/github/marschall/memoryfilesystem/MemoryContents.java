package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.AutoReleaseLock.autoRelease;
import static java.lang.Math.min;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class MemoryContents {
	
  //TODO update m, c, a times

  /**
   * The object header size of an array. Two words (flags &amp; class oop)
   * plus array size (2 *64 bit + 32 bit on 64 bit, 2 *32 bit + 32 bit on 32 bit).
   */
  private static final int ARRAY_HEADER = 8 + 8 + 4;

  static final int BLOCK_SIZE = 4096 - ARRAY_HEADER; //make sure it fits into a 4k memory region

  // TODO
  // byte[] first
  // byte[][] second
  // byte[][][] third
  private byte[][] blocks;

  private long size;

  private int blocksAllocated;

  private final ReadWriteLock lock;

  MemoryContents(int initialBlocks) {
    this.lock = new ReentrantReadWriteLock();
    this.blocks = new byte[initialBlocks][];
    this.size = 0L;
    this.blocksAllocated = 0;
  }

  SeekableByteChannel newChannel(boolean readable, boolean writable) {
    return new NonAppendingBlockChannel(this, readable, writable);
  }

  SeekableByteChannel newAppendingChannel(boolean readable) {
    return new AppendingBlockChannel(this, readable, this.size);
  }

  long size() {
    try (AutoRelease lock = this.readLock()) {
      return this.size;
    }
  }

  private AutoRelease readLock() {
    return autoRelease(this.lock.readLock());
  }

  private AutoRelease writeLock() {
    return autoRelease(this.lock.writeLock());
  }

  int read(ByteBuffer dst, long position) throws IOException {
    try (AutoRelease lock = this.readLock()) {
      if (position >= this.size) {
        return -1;
      }
      long remaining = dst.remaining();
      int toRead = (int) min(min(this.size - position, remaining), Integer.MAX_VALUE);
      int currentBlock = (int) (position / BLOCK_SIZE);
      int startIndexInBlock = (int) (position - ((long) currentBlock * (long) BLOCK_SIZE));
      int read = 0;
      while (read < toRead) {
        int lengthInBlock = (int) min((long) BLOCK_SIZE - (long) startIndexInBlock, (long) toRead - (long) read);

        dst.put(this.blocks[currentBlock], startIndexInBlock, lengthInBlock);
        read += lengthInBlock;

        startIndexInBlock = 0;
        currentBlock += 1;
      }
      return read;

    }
  }

  int write(ByteBuffer src, long position) {
    try (AutoRelease lock = this.writeLock()) {
      long remaining = src.remaining();
      this.ensureCapacity(position + remaining);

      int toWrite = (int) min(remaining, Integer.MAX_VALUE);
      int currentBlock = (int) (position / BLOCK_SIZE);
      int startIndexInBlock = (int) (position - ((long) currentBlock * (long) BLOCK_SIZE));
      int written = 0;
      while (written < toWrite) {
        int lengthInBlock = (int) min((long) BLOCK_SIZE - (long) startIndexInBlock, (long) toWrite - (long) written);

        src.get(this.blocks[currentBlock], startIndexInBlock, lengthInBlock);
        written += lengthInBlock;

        startIndexInBlock = 0;
        currentBlock += 1;
      }
      if (position > this.size) {
        // REVIEW, possibility to fill with random data
        this.size = position;
      }
      this.size += written;
      return written;
    }
  }


  int writeAtEnd(ByteBuffer src) {
    try (AutoRelease lock = this.writeLock()) {
      return this.write(src, this.size);
    }
  }

  void truncate(long newSize) {
    try (AutoRelease lock = this.writeLock()) {
      if (newSize < this.size) {
        this.size = newSize;
      }
    }

  }

  private void ensureCapacity(long capacity) {
    int blocksRequired;
    if (capacity == 0L) {
      blocksRequired = 1;
    } else {
      blocksRequired = (int) ((capacity - 1L)/ BLOCK_SIZE) + 1;
    }
    if (blocksRequired > this.blocks.length) {
      int newNumberOfBlocks = Math.max(this.blocks.length * 2, blocksRequired);
      byte[][] newBlocks = new byte[newNumberOfBlocks][];
      System.arraycopy(this.blocks, 0, newBlocks, 0, this.blocksAllocated);
      this.blocks = newBlocks;
    }
    if (blocksRequired > this.blocksAllocated) {
      for (int i = this.blocksAllocated; i < blocksRequired; ++i) {
        this.blocks[i] = new byte[BLOCK_SIZE];
        this.blocksAllocated += 1;
      }
    }
  }


}

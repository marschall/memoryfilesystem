package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.AutoReleaseLock.autoRelease;
import static java.lang.Math.min;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
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
  // byte[][][] doubleIndirectBlocks

  /**
   * To store the contents efficiently we store the first {@value #BLOCK_SIZE}
   * bytes in a {@value #BLOCK_SIZE} direct {@code byte[]}. The next
   * {@value #BLOCK_SIZE} * {@value #BLOCK_SIZE} bytes go into a indirect
   * {@code byte[][]} that is lazily allocated.
   */
  private byte[] directBlock;
  private byte[][] indirectBlocks;

  private long size;

  private int indirectBlocksAllocated;

  private final ReadWriteLock lock;

  MemoryContents() {
    this(0);
  }

  MemoryContents(int initialBlocks) {
    this.lock = new ReentrantReadWriteLock();
    if (initialBlocks == 0) {
      // TODO could be made smaller
      this.directBlock = new byte[BLOCK_SIZE];
    } else {
      this.directBlock = new byte[BLOCK_SIZE];
    }
    if (initialBlocks > 1) {
      this.indirectBlocks = new byte[BLOCK_SIZE][];
      for (int i = 0; i < initialBlocks - 1; ++i) {
        this.indirectBlocks[i] = new byte[BLOCK_SIZE];
      }
      this.indirectBlocksAllocated = initialBlocks - 1;
    }
    this.size = 0L;
    // only the single direct block
  }

  InputStream newInputStream() {
    return new BlockInputStream(this);
  }

  OutputStream newOutputStream() {
    return new NonAppendingBlockOutputStream(this);
  }

  OutputStream newAppendingOutputStream() {
    return new AppendingBlockOutputStream(this);
  }

  FileChannel newChannel(boolean readable, boolean writable) {
    return new NonAppendingBlockChannel(this, readable, writable);
  }

  FileChannel newAppendingChannel(boolean readable) {
    return new AppendingBlockChannel(this, readable, this.size);
  }

  long size() {
    try (AutoRelease lock = this.readLock()) {
      return this.size;
    }
  }

  private byte[] getBlock(int currentBlock) {
    if (currentBlock == 0) {
      return this.directBlock;
    } else {
      return this.indirectBlocks[currentBlock - 1];
    }
  }

  private AutoRelease readLock() {
    return autoRelease(this.lock.readLock());
  }

  private AutoRelease writeLock() {
    return autoRelease(this.lock.writeLock());
  }

  long read(ByteBuffer dst, long position, long maximum) throws IOException {
    try (AutoRelease lock = this.readLock()) {
      if (position >= this.size) {
        return -1L;
      }
      long remaining = dst.remaining();
      long toRead = min(min(this.size - position, remaining), maximum);
      int currentBlock = (int) (position / BLOCK_SIZE);
      int startIndexInBlock = (int) (position - ((long) currentBlock * (long) BLOCK_SIZE));
      long read = 0L;
      while (read < toRead) {
        int lengthInBlock = (int) min((long) BLOCK_SIZE - (long) startIndexInBlock, toRead - read);

        dst.put(this.getBlock(currentBlock), startIndexInBlock, lengthInBlock);
        read += lengthInBlock;

        startIndexInBlock = 0;
        currentBlock += 1;
      }
      return read;
    }
  }

  int readShort(ByteBuffer dst, long position) throws IOException {
    return (int) this.read(dst, position, Integer.MAX_VALUE);
  }

  int read(byte[] dst, long position, int off, int len) throws IOException {
    try (AutoRelease lock = this.readLock()) {
      if (position >= this.size) {
        return -1;
      }
      int toRead = (int) min(min(this.size - position, len), Integer.MAX_VALUE);
      int currentBlock = (int) (position / BLOCK_SIZE);
      int startIndexInBlock = (int) (position - ((long) currentBlock * (long) BLOCK_SIZE));
      int read = 0;
      while (read < toRead) {
        int lengthInBlock = (int) min((long) BLOCK_SIZE - (long) startIndexInBlock, (long) toRead - (long) read);

        byte[] block = this.getBlock(currentBlock);
        System.arraycopy(block, startIndexInBlock, dst, off + read, lengthInBlock);
        read += lengthInBlock;

        startIndexInBlock = 0;
        currentBlock += 1;
      }
      return read;
    }
  }

  long transferFrom(ReadableByteChannel src, long position, long count) {
    try (AutoRelease lock = this.writeLock()) {
      this.ensureCapacity(position + count);
      long transferred = 0L;
      long toTransfer = min(count, this.size - position);

      return transferred;
    }
  }

  long transferTo(WritableByteChannel target, long position, long count) {
    try (AutoRelease lock = this.readLock()) {
      long transferred = 0L;

      return transferred;
    }
  }

  long write(ByteBuffer src, long position, long maximum) {
    try (AutoRelease lock = this.writeLock()) {
      long remaining = src.remaining();
      this.ensureCapacity(position + remaining);

      long toWrite = min(remaining, maximum);
      int currentBlock = (int) (position / BLOCK_SIZE);
      int startIndexInBlock = (int) (position - ((long) currentBlock * (long) BLOCK_SIZE));
      long written = 0L;
      while (written < toWrite) {
        int lengthInBlock = (int) min((long) BLOCK_SIZE - (long) startIndexInBlock, toWrite - written);

        src.get(this.getBlock(currentBlock), startIndexInBlock, lengthInBlock);
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

  int writeShort(ByteBuffer src, long position) {
    return (int) this.write(src, position, Integer.MAX_VALUE);
  }

  int write(byte[] src, long position, int off, int len) {
    try (AutoRelease lock = this.writeLock()) {
      this.ensureCapacity(position + len);

      int toWrite = min(len, Integer.MAX_VALUE);
      int currentBlock = (int) (position / BLOCK_SIZE);
      int startIndexInBlock = (int) (position - ((long) currentBlock * (long) BLOCK_SIZE));
      int written = 0;
      while (written < toWrite) {
        int lengthInBlock = (int) min((long) BLOCK_SIZE - (long) startIndexInBlock, (long) toWrite - (long) written);

        byte[] block = this.getBlock(currentBlock);
        System.arraycopy(src, off + written, block, startIndexInBlock, lengthInBlock);
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

  long writeAtEnd(ByteBuffer src, long maximum) {
    try (AutoRelease lock = this.writeLock()) {
      return this.write(src, this.size, maximum);
    }
  }

  int writeAtEnd(ByteBuffer src) {
    try (AutoRelease lock = this.writeLock()) {
      return this.writeShort(src, this.size);
    }
  }

  int writeAtEnd(byte[] src, int off, int len) {
    try (AutoRelease lock = this.writeLock()) {
      return this.write(src, this.size, off, len);
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
    // if direct block is enough do nothing
    if (capacity <= BLOCK_SIZE) {
      return;
    }

    // lazily allocate indirect blocks
    if (this.indirectBlocks == null) {
      this.indirectBlocks = new byte[BLOCK_SIZE][];
    }

    int blocksRequired = (int) ((capacity - 1L)/ BLOCK_SIZE); // consider already present direct block, don't add + 1

    if (blocksRequired > BLOCK_SIZE) {
      // FIXME implement double indirect addressing
      throw new AssertionError("files bigger than 16GB not yet supported");
    }

    if (blocksRequired > this.indirectBlocksAllocated) {
      for (int i = this.indirectBlocksAllocated; i < blocksRequired; ++i) {
        this.indirectBlocks[i] = new byte[BLOCK_SIZE];
        this.indirectBlocksAllocated += 1;
      }
    }
  }


}

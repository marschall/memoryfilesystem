package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.AutoReleaseLock.autoRelease;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

final class MemoryInode {

  /*
   * It turned out to be easier to implement the contents in this class
   * rather than a separate class since it needs access to a, c, m times
   * and update the open count.
   *
   * #transferTo and #transferFrom are candidates for deadlocks since they
   * acquire two locks without ordering. However one is a read lock
   * and the other is a write lock. So I "think" we're fine for now.
   */

  /**
   * The object header size of an array. Two words (flags &amp; class oop)
   * plus array size (2 *64 bit + 32 bit on 64 bit, 2 *32 bit + 32 bit on 32 bit).
   */
  private static final int ARRAY_HEADER = 8 + 8 + 4;

  static final int BLOCK_SIZE = 4096 - ARRAY_HEADER; //make sure it fits into a 4k memory region
  static final int NUMBER_OF_BLOCKS = BLOCK_SIZE;

  // lazily allocated, most files probably won't need this
  private LockSet lockSet;

  /**
   * To store the contents efficiently we store the first {@value #BLOCK_SIZE}
   * bytes in a {@value #BLOCK_SIZE} direct {@code byte[]}. The next
   * {@value #NUMBER_OF_BLOCKS} * {@value #BLOCK_SIZE} bytes go into a indirect
   * {@code byte[][]} that is lazily allocated.
   */
  private byte[] directBlock;
  private byte[][] indirectBlocks;

  // TODO
  // byte[][][] doubleIndirectBlocks

  // TODO max link count

  private long size;

  private int indirectBlocksAllocated;

  private final ReadWriteLock lock;

  MemoryInode(int initialBlocks) {
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
  }

  MemoryInode(MemoryInode other) {
    this.lock = new ReentrantReadWriteLock();
    if (other.directBlock != null) {
      this.directBlock = other.directBlock.clone();
    }

    if (other.indirectBlocks != null) {
      this.indirectBlocks = other.indirectBlocks.clone();
      for (int i = 0; i < other.indirectBlocksAllocated; ++i) {
        this.indirectBlocks[i] = other.indirectBlocks[i].clone();
      }
    }
    this.indirectBlocksAllocated = other.indirectBlocksAllocated;

    this.size = other.size;
  }

  AutoRelease readLock() {
    return autoRelease(this.lock.readLock());
  }

  AutoRelease writeLock() {
    return autoRelease(this.lock.writeLock());
  }

  long size() {
    try (AutoRelease lock = this.readLock()) {
      return this.size;
    }
  }

  long read(ByteBuffer dst, long position, long maximum) {
    try (AutoRelease lock = this.readLock()) {
      if (position >= this.size) {
        return -1L;
      }
      long remaining = dst.remaining();
      long toRead = min(min(this.size - position, remaining), maximum);
      int currentBlock = (int) (position / BLOCK_SIZE);
      int startIndexInBlock = (int) (position - (currentBlock * (long) BLOCK_SIZE));
      long read = 0L;
      while (read < toRead) {
        int lengthInBlock = (int) min(BLOCK_SIZE - startIndexInBlock, toRead - read);

        byte[] block = this.getBlock(currentBlock);
        dst.put(block, startIndexInBlock, lengthInBlock);
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

  int read(byte[] dst, long position, int off, int len) {
    try (AutoRelease lock = this.readLock()) {
      if (position >= this.size) {
        return -1;
      }
      int toRead = (int) min(min(this.size - position, len), Integer.MAX_VALUE);
      int currentBlock = (int) (position / BLOCK_SIZE);
      int startIndexInBlock = (int) (position - (currentBlock * (long) BLOCK_SIZE));
      int read = 0;
      while (read < toRead) {
        int lengthInBlock = min(BLOCK_SIZE - startIndexInBlock, toRead - read);

        byte[] block = this.getBlock(currentBlock);
        System.arraycopy(block, startIndexInBlock, dst, off + read, lengthInBlock);
        read += lengthInBlock;

        startIndexInBlock = 0;
        currentBlock += 1;
      }
      return read;
    }
  }

  long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
    try (AutoRelease lock = this.writeLock()) {
      this.ensureCapacity(position + count);
      long transferred = 0L;
      long toTransfer = count;

      int currentBlock = (int) (position / BLOCK_SIZE);
      int startIndexInBlock = (int) (position - (currentBlock * (long) BLOCK_SIZE));
      while (transferred < toTransfer) {
        int lengthInBlock = (int) min(BLOCK_SIZE - startIndexInBlock, toTransfer - transferred);

        byte[] block = this.getBlock(currentBlock);
        // We can either allocate a new ByteBuffer for every iteration or keep
        // the buffer and copy the contents into it.
        // Since ByteBuffer objects are quite small and don't copy the contents
        // of the backing array allocating a ByteBuffer is probably cheaper.
        ByteBuffer buffer = ByteBuffer.wrap(block, startIndexInBlock, lengthInBlock);
        readFully(buffer, src, lengthInBlock);
        transferred += lengthInBlock;

        startIndexInBlock = 0;
        currentBlock += 1;
      }
      // REVIEW, possibility to fill with random data
      this.size = max(this.size, position + transferred);
      return transferred;
    }
  }

  long transferTo(WritableByteChannel target, long position, long count) throws IOException {
    try (AutoRelease lock = this.readLock()) {
      long transferred = 0L;
      long toTransfer = min(count, this.size - position);

      int currentBlock = (int) (position / BLOCK_SIZE);
      int startIndexInBlock = (int) (position - (currentBlock * (long) BLOCK_SIZE));
      while (transferred < toTransfer) {
        int lengthInBlock = (int) min(BLOCK_SIZE - startIndexInBlock, toTransfer - transferred);

        byte[] block = this.getBlock(currentBlock);
        // We can either allocate a new ByteBuffer for every iteration or keep
        // the buffer and copy the contents into it.
        // Since ByteBuffer objects are quite small and don't copy the contents
        // of the backing array allocating a ByteBuffer is probably cheaper.
        ByteBuffer buffer = ByteBuffer.wrap(block, startIndexInBlock, lengthInBlock);
        writeFully(buffer, target, lengthInBlock);
        transferred += lengthInBlock;

        startIndexInBlock = 0;
        currentBlock += 1;
      }

      return transferred;
    }
  }



  long transferTo(OutputStream target, long position) throws IOException {
    try (AutoRelease lock = this.readLock()) {
      long transferred = 0L;
      long toTransfer = this.size - position;

      int currentBlock = (int) (position / BLOCK_SIZE);
      int startIndexInBlock = (int) (position - (currentBlock * (long) BLOCK_SIZE));
      while (transferred < toTransfer) {
        int lengthInBlock = (int) min(BLOCK_SIZE - startIndexInBlock, toTransfer - transferred);

        byte[] block = this.getBlock(currentBlock);

        target.write(block, startIndexInBlock, lengthInBlock);
        transferred += lengthInBlock;

        startIndexInBlock = 0;
        currentBlock += 1;
      }

      return transferred;
    }
  }

  long write(ByteBuffer src, long position, long maximum) {
    try (AutoRelease lock = this.writeLock()) {
      long remaining = src.remaining();
      this.ensureCapacity(position + remaining);

      long toWrite = min(remaining, maximum);
      int currentBlock = (int) (position / BLOCK_SIZE);
      int startIndexInBlock = (int) (position - (currentBlock * (long) BLOCK_SIZE));
      long written = 0L;
      while (written < toWrite) {
        int lengthInBlock = (int) min((long) BLOCK_SIZE - startIndexInBlock, toWrite - written);

        byte[] block = this.getBlock(currentBlock);
        src.get(block, startIndexInBlock, lengthInBlock);
        written += lengthInBlock;

        startIndexInBlock = 0;
        currentBlock += 1;
      }
      // REVIEW, possibility to fill with random data
      this.size = max(this.size, position + written);
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
      int startIndexInBlock = (int) (position - (currentBlock * (long) BLOCK_SIZE));
      int written = 0;
      while (written < toWrite) {
        int lengthInBlock = min(BLOCK_SIZE - startIndexInBlock, toWrite - written);

        byte[] block = this.getBlock(currentBlock);
        System.arraycopy(src, off + written, block, startIndexInBlock, lengthInBlock);
        written += lengthInBlock;

        startIndexInBlock = 0;
        currentBlock += 1;
      }
      // REVIEW, possibility to fill with random data
      this.size = max(this.size, position + written);
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

  MemoryFileLock tryLock(MemoryFileLock lock) {
    try (AutoRelease autoRelease = this.writeLock()) {
      return this.lockSet().tryLock(lock);
    }
  }

  MemoryFileLock lock(MemoryFileLock lock) throws IOException {
    try (AutoRelease autoRelease = this.writeLock()) {
      return this.lockSet().lock(lock);
    }
  }

  void unlock(MemoryFileLock lock) {
    try (AutoRelease autoRelease = this.writeLock()) {
      this.lockSet.remove(lock);
    }
  }

  private LockSet lockSet() {
    if (this.lockSet == null) {
      this.lockSet = new LockSet();
    }
    return this.lockSet;
  }

  //  void accessed();
  //
  //  void modified();

  private byte[] getBlock(int currentBlock) {
    if (currentBlock == 0) {
      return this.directBlock;
    } else {
      return this.indirectBlocks[currentBlock - 1];
    }
  }

  private static int writeFully(ByteBuffer src, WritableByteChannel target, int toWrite) throws IOException {
    int written = 0;
    while (written < toWrite) {
      written += target.write(src);
    }
    return written;
  }

  private static int readFully(ByteBuffer src, ReadableByteChannel target, int toRead) throws IOException {
    int read = 0;
    while (read < toRead) {
      read += target.read(src);
    }
    return read;
  }

  private void ensureCapacity(long capacity) {
    // if direct block is enough do nothing
    if (capacity <= BLOCK_SIZE) {
      return;
    }

    // lazily allocate indirect blocks
    if (this.indirectBlocks == null) {
      this.indirectBlocks = new byte[NUMBER_OF_BLOCKS][];
    }

    int blocksRequired = (int) ((capacity - 1L)/ BLOCK_SIZE); // consider already present direct block, don't add + 1

    if (blocksRequired > NUMBER_OF_BLOCKS) {
      // FIXME implement double indirect addressing
      throw new AssertionError("files bigger than 16MB not yet supported");
    }

    if (blocksRequired > this.indirectBlocksAllocated) {
      for (int i = this.indirectBlocksAllocated; i < blocksRequired; ++i) {
        this.indirectBlocks[i] = new byte[BLOCK_SIZE];
        this.indirectBlocksAllocated += 1;
      }
    }
  }

}

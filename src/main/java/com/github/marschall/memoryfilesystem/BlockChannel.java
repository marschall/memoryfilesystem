package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.AutoReleaseLock.autoRelease;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

abstract class BlockChannel extends FileChannel {

  volatile long position;

  final MemoryContents memoryContents;

  final boolean readable;

  /**
   * The {@link java.nio.channels.WritableByteChannel} documentation says
   * only one write operation may be in progress on a channel at a time.
   * 
   * The {@link java.nio.channels.ReadableByteChannel} documentation says
   * only one read operation may be in progress on a channel at a time.
   */
  private final Lock lock;

  /**
   * Lazily allocated.
   */
  private Set<MemoryFileLock> fileLocks;

  private final Path pathToDelete;


  BlockChannel(MemoryContents memoryContents, boolean readable, boolean deleteOnClose, Path path) {
    this.memoryContents = memoryContents;
    this.readable = readable;
    this.lock = new ReentrantLock();
    if (deleteOnClose) {
      this.pathToDelete = path;
    } else {
      this.pathToDelete = null;
    }
  }

  void closedCheck() throws ClosedChannelException {
    if (!this.isOpen()) {
      throw new ClosedChannelException();
    }
  }

  abstract void writeCheck() throws ClosedChannelException;

  private void readCheck() throws ClosedChannelException {
    this.closedCheck();
    if (!this.readable) {
      throw new NonReadableChannelException();
    }
  }

  AutoRelease writeLock() throws ClosedChannelException {
    this.writeCheck();
    return autoRelease(this.lock);
  }

  private AutoRelease readLock() throws ClosedChannelException {
    this.readCheck();
    return autoRelease(this.lock);
  }


  @Override
  public int read(ByteBuffer dst) throws IOException {
    try (AutoRelease lock = this.readLock()) {
      int read = this.memoryContents.readShort(dst, this.position);
      if (read != -1) {
        this.position += read;
      }
      return read;
    }
  }

  @Override
  public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
    this.validateOffsetAndLength(dsts, offset, length);
    try (AutoRelease lock = this.readLock()) {
      // we have to take care this value does not overflow since a single buffer
      // can hold more than Integer.MAX_VALUE bytes and this method returns a long
      long totalRead = 0L;
      for (int i = 0; i < length; ++i) {
        if (totalRead == Long.MAX_VALUE) {
          break;
        }

        // we have to make sure a buffers capacity is exhausted before reading into the
        // next buffer so we use the method that returns a long
        long read = this.memoryContents.read(dsts[offset + 1], this.position, Long.MAX_VALUE - totalRead);
        if (read != -1) {
          // we could read data, update position and total counter
          this.position += read;
          totalRead += read;
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
      return totalRead;
    }
  }

  void validatePositionAndCount(long position, long count) {
    if (position < 0) {
      throw new IllegalArgumentException("position must be positive");
    }
    if (count < 0) {
      throw new IllegalArgumentException("count must be positive");
    }
  }

  void validateOffsetAndLength(ByteBuffer[] buffers, int offset, int length) {
    if (offset < 0) {
      throw new IndexOutOfBoundsException("offset must not be negative");
    }
    if (offset >= buffers.length) {
      throw new IndexOutOfBoundsException("offset must be smaller than " + buffers.length);
    }
    if (length < 0) {
      throw new IndexOutOfBoundsException("length must not be negative");
    }
    if (length > buffers.length - offset) {
      throw new IndexOutOfBoundsException("length too large");
    }
  }

  @Override
  public int read(ByteBuffer dst, long position) throws IOException {
    try (AutoRelease lock = this.readLock()) {
      return this.memoryContents.readShort(dst, position);
    }
  }

  @Override
  public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
    // TODO more validation
    this.validatePositionAndCount(position, count);
    try (AutoRelease lock = this.readLock()) {
      return this.memoryContents.transferTo(target, position, count);
    }
  }

  @Override
  public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
    throw new UnsupportedOperationException("memory file system does not support mmapped IO");
  }

  @Override
  public long position() throws IOException {
    this.closedCheck();
    return this.position;
  }


  @Override
  public FileChannel position(long newPosition) throws IOException {
    if (newPosition < 0L) {
      throw new IllegalArgumentException("only a non-negative values are allowed, " + newPosition + " is invalid");
    }
    this.closedCheck();
    try (AutoRelease autoRelease = autoRelease(this.lock)) {
      this.position = newPosition;
    }
    return this;
  }


  @Override
  public long size() throws IOException {
    this.closedCheck();
    return this.memoryContents.size();
  }

  MemoryFileLock lock(MemoryFileLock l) throws IOException {
    try (AutoRelease lock = this.writeLock()) {
      MemoryFileLock fileLock = this.memoryContents.lock(l);
      this.addLock(fileLock);
      return fileLock;
    }
  }

  FileLock tryLock(MemoryFileLock l) throws IOException {
    try (AutoRelease lock = this.writeLock()) {
      MemoryFileLock fileLock = this.memoryContents.tryLock(l);
      if (fileLock != null) {
        this.addLock(fileLock);
      }
      return fileLock;
    }
  }

  @Override
  public FileLock lock(long position, long size, boolean shared) throws IOException {
    return this.lock(new MemoryFileLock(this, position, size, shared));
  }

  @Override
  public FileLock tryLock(long position, long size, boolean shared) throws IOException {
    return this.tryLock(new MemoryFileLock(this, position, size, shared));
  }

  private void addLock(MemoryFileLock fileLock) {
    if (this.fileLocks == null) {
      this.fileLocks = new HashSet<>();
    }
    this.fileLocks.add(fileLock);
  }

  void removeLock(MemoryFileLock fileLock) throws IOException {
    try (AutoRelease lock = this.writeLock()) {
      this.fileLocks.remove(fileLock);
      this.memoryContents.unlock(fileLock);
    }
  }

  @Override
  protected void implCloseChannel() throws IOException {
    try (AutoRelease lock = autoRelease(this.lock)) {
      // update atime and mtime
      this.force(true);
      if (this.fileLocks != null) {
        for (MemoryFileLock fileLock : this.fileLocks) {
          this.memoryContents.unlock(fileLock);
        }
      }
      this.memoryContents.closedChannel(this.pathToDelete);
    }
  }

}

package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.AutoReleaseLock.autoRelease;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

abstract class BlockChannel implements SeekableByteChannel {

  volatile long position;

  private final ClosedChannelChecker checker;

  final MemoryContents memoryContents;

  private final boolean readable;

  /**
   * The {@link java.nio.channels.WritableByteChannel} documentation says
   * only one write operation may be in progress on a channel at a time.
   * 
   * The {@link java.nio.channels.ReadableByteChannel} documentation says
   * only one read operation may be in progress on a channel at a time.
   */
  private final Lock lock;


  BlockChannel(MemoryContents memoryContents, boolean readable, boolean writable) {
    this.memoryContents = memoryContents;
    this.readable = readable;
    this.checker = new ClosedChannelChecker();
    this.lock = new ReentrantLock();
  }

  abstract void writeCheck();

  private void readCheck() {
    if (!this.readable) {
      throw new NonReadableChannelException();
    }
  }

  AutoRelease writeLock() {
    this.writeCheck();
    return autoRelease(this.lock);
  }

  private AutoRelease readLock() {
    this.readCheck();
    return autoRelease(this.lock);
  }


  @Override
  public boolean isOpen() {
    return this.checker.isOpen();
  }


  @Override
  public int read(ByteBuffer dst) throws IOException {
    try (AutoRelease lock = this.readLock()) {
      int read = this.memoryContents.read(dst, this.position);
      if (read != -1) {
        this.position += read;
      }
      return read;
    }
  }



  @Override
  public long position() throws IOException {
    this.checker.check();
    return this.position;
  }


  @Override
  public SeekableByteChannel position(long newPosition) throws IOException {
    if (newPosition < 0L) {
      throw new IllegalArgumentException("only a non-negative values are allowed, " + newPosition + " is invalid");
    }
    this.checker.check();
    try (AutoRelease lock = AutoReleaseLock.autoRelease(this.lock)) {
      this.position = newPosition;
    }
    return this;
  }


  @Override
  public long size() throws IOException {
    this.checker.check();
    return this.memoryContents.size();
  }


  @Override
  public void close() throws IOException {
    // REVIEW the spec of Channel says we should block and wait for the first invocation to finish
    this.checker.close();
  }


}

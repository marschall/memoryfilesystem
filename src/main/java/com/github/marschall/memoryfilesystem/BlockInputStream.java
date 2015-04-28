package com.github.marschall.memoryfilesystem;

import static java.lang.Math.min;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

final class BlockInputStream extends InputStream {

  /**
   * The maximum number of bytes to skip.
   */
  private static final int MAX_SKIP_SIZE = 2048;

  private final MemoryContents memoryContents;
  private final ClosedStreamChecker checker;
  private final AtomicLong position;
  private final Path path;
  private final boolean deleteOnClose;
  //
  //  private final Lock markLock;
  //
  //  private long markPositon;
  //  private int readLimit;


  BlockInputStream(MemoryContents memoryContents, boolean deleteOnClose, Path path) {
    this.memoryContents = memoryContents;
    this.deleteOnClose = deleteOnClose;
    this.checker = new ClosedStreamChecker();
    this.position = new AtomicLong(0L);
    this.path = path;
    //    this.markLock = new ReentrantLock();
    //    this.readLimit = -1;
    //    this.markPositon = -1L;
  }


  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    this.checker.check(this.path);
    boolean success = false;
    int read = 0;
    while (!success) {
      long positionBefore = this.position.get();
      read = this.memoryContents.read(b, positionBefore, off, len);
      if (read == -1) {
        return read;
      }
      success = this.position.compareAndSet(positionBefore, positionBefore + read);
    }
    return read;
  }

  @Override
  public long skip(long n) throws IOException {
    this.checker.check(this.path);
    long positionBefore = this.position.get();
    long fileSize = this.memoryContents.size();
    // do not skip more than MAX_SKIP_SIZE
    // this intentionally introduces a subtle bug in code that doesn't check
    // for the return value of #skip
    // REVIEW subtle issue when fileSize - positionBefore becomes negative
    // due to concurrent updates
    long skipped = min(min(n, fileSize - positionBefore), MAX_SKIP_SIZE);
    this.position.compareAndSet(positionBefore, positionBefore + skipped);
    return skipped;
  }

  @Override
  public int available() throws IOException {
    this.checker.check(this.path);
    long available = this.memoryContents.size() - this.position.get();
    if (available > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    } else if (available > 1L) {
      // introduce a subtle but in code that assumes #available() returns
      // everything untile the end
      return (int) (available -1);
    } else {
      return (int) available;
    }
  }

  @Override
  public void close() throws IOException {
    this.checker.close();
    this.memoryContents.accessed();
    this.memoryContents.closedStream(this.path, this.deleteOnClose);
  }

  // FileInputStream doesn't support marks so neither do we


  //  @Override
  //  public void mark(int readlimit) {
  //    try (AutoRelease lock = AutoReleaseLock.autoRelease(this.markLock)) {
  //      if (readlimit <= 0) {
  //        throw new IllegalArgumentException("read limit must be positive");
  //      }
  //      this.markPositon = this.position.get();
  //      this.readLimit = readlimit;
  //    }
  //  }
  //
  //  @Override
  //  public void reset() throws IOException {
  //    // REVIEW this doesn't actually guarantee the same bytes are read
  //    // by concurrent access, I'm not sure this complies with the specification
  //    // the specification doesn't say anything about concurrent access
  //    try (AutoRelease lock = AutoReleaseLock.autoRelease(this.markLock)) {
  //      if (this.readLimit == -1) {
  //        throw new IOException("#mark has not been called");
  //      }
  //      if (this.position.get() > this.markPositon + this.readLimit) {
  //        throw new IOException("readlimit has passed");
  //      }
  //      this.position.set(this.markPositon);
  //    }
  //  }

  @Override
  public int read() throws IOException {
    byte[] data = new byte[1];
    int read = this.read(data);
    if (read == -1) {
      return read;
    } else {
      return data[0] & 0xff;
    }

  }

}

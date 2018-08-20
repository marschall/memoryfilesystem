package com.github.marschall.memoryfilesystem;

import static java.lang.Math.min;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

final class BlockInputStream extends InputStream {

  /**
   * The maximum number of bytes to skip.
   */
  private static final int MAX_SKIP_SIZE = 2048;

  static final AtomicLongFieldUpdater<BlockInputStream> POSITION_UPDATER =
          AtomicLongFieldUpdater.newUpdater(BlockInputStream.class, "position");

  private final MemoryContents memoryContents;
  private final ClosedStreamChecker checker;
  @SuppressWarnings("unused") // POSITION_UPDATER
  private volatile long position;
  private final Path path;
  private final boolean deleteOnClose;
  //
  //  private final Lock markLock;
  //
  //  private long markPosition;
  //  private int readLimit;


  BlockInputStream(MemoryContents memoryContents, boolean deleteOnClose, Path path) {
    this.memoryContents = memoryContents;
    this.deleteOnClose = deleteOnClose;
    this.checker = new ClosedStreamChecker();
    POSITION_UPDATER.set(this, 0L);
    this.path = path;
    //    this.markLock = new ReentrantLock();
    //    this.readLimit = -1;
    //    this.markPosition = -1L;
  }


  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    this.checker.check(this.path);
    boolean success = false;
    int read = 0;
    while (!success) {
      long positionBefore = POSITION_UPDATER.get(this);
      read = this.memoryContents.read(b, positionBefore, off, len);
      if (read == -1) {
        return read;
      }
      success = POSITION_UPDATER.compareAndSet(this, positionBefore, positionBefore + read);
    }
    return read;
  }

  @Override
  public long skip(long n) throws IOException {
    this.checker.check(this.path);
    long fileSize = this.memoryContents.size();
    long skipped = 0L;
    boolean success = false;
    while (!success) {
      long positionBefore = POSITION_UPDATER.get(this);
      // do not skip more than MAX_SKIP_SIZE
      // this intentionally introduces a subtle bug in code that doesn't check
      // for the return value of #skip
      skipped = min(min(n, fileSize - positionBefore), MAX_SKIP_SIZE);
      if (skipped < 0L) {
        // file size changed due to concurrent access
        fileSize = this.memoryContents.size();
        continue;
      }
      success = POSITION_UPDATER.compareAndSet(this, positionBefore, positionBefore + skipped);
    }
    return skipped;
  }

  @Override
  public int available() throws IOException {
    this.checker.check(this.path);
    long available = this.memoryContents.size() - POSITION_UPDATER.get(this);
    if (available > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    } else if (available > 1L) {
      // introduce a subtle bug in code that assumes #available() returns
      // everything until the end
      return (int) (available -1);
    } else {
      return (int) available;
    }
  }

  @Override
  public void close() {
    if (this.checker.close()) {
      this.memoryContents.accessed();
      this.memoryContents.closedStream(this.path, this.deleteOnClose);
    }
  }

  // FileInputStream doesn't support marks so neither do we


  //  @Override
  //  public void mark(int readlimit) {
  //    try (AutoRelease lock = AutoReleaseLock.autoRelease(this.markLock)) {
  //      if (readlimit <= 0) {
  //        throw new IllegalArgumentException("read limit must be positive");
  //      }
  //      this.markPosition = this.position.get();
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
  //      if (this.position.get() > this.markPosition + this.readLimit) {
  //        throw new IOException("readlimit has passed");
  //      }
  //      this.position.set(this.markPosition);
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

  // Java 9 method, has to compile under Java 1.7 so no @Override
  public long transferTo​(OutputStream out) throws IOException {
    this.checker.check(this.path);
    long positionBefore = POSITION_UPDATER.get(this);
    long written = this.memoryContents.transferTo(out, positionBefore);
    POSITION_UPDATER.set(this, this.memoryContents.size());
    return written;
  }

}

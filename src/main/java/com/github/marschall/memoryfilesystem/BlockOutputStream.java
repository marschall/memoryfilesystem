package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;

final class BlockOutputStream extends OutputStream {

  private final MemoryContents memoryContents;
  private final ClosedStreamChecker checker;
  private final AtomicLong position;

  BlockOutputStream(MemoryContents memoryContents) {
    this.memoryContents = memoryContents;
    this.checker = new ClosedStreamChecker();
    this.position = new AtomicLong(0L);
  }

  @Override
  public void write(int b) throws IOException {
    // TODO Auto-generated method stub
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    // TODO Auto-generated method stub
    super.write(b, off, len);
  }

  @Override
  public void flush() throws IOException {
    // TODO Auto-generated method stub
    super.flush();
    // TODO atime, mtime
  }

  @Override
  public void close() throws IOException {
    this.checker.close();

    // TODO atime, mtime
  }



}

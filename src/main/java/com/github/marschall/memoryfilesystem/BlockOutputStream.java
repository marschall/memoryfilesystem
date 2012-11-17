package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.io.OutputStream;

abstract class BlockOutputStream extends OutputStream {

  final MemoryContents memoryContents;
  final ClosedStreamChecker checker;


  BlockOutputStream(MemoryContents memoryContents) {
    this.memoryContents = memoryContents;
    this.checker = new ClosedStreamChecker();
  }

  @Override
  public void write(int b) throws IOException {
    byte[] data = new byte[]{(byte) b};
    this.write(data, 0, 1);
  }

  @Override
  public void flush() throws IOException {
    super.flush();
    // TODO atime, mtime
  }

  @Override
  public void close() throws IOException {
    this.checker.close();
    // TODO atime, mtime
  }

}

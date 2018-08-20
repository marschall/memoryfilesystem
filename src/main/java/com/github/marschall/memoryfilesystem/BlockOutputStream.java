package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

abstract class BlockOutputStream extends OutputStream {

  final MemoryContents memoryContents;
  final ClosedStreamChecker checker;
  final Path path;
  private final boolean deleteOnClose;


  BlockOutputStream(MemoryContents memoryContents, boolean deleteOnClose, Path path) {
    this.memoryContents = memoryContents;
    this.deleteOnClose = deleteOnClose;
    this.checker = new ClosedStreamChecker();
    this.path = path;
  }

  @Override
  public void write(int b) throws IOException {
    byte[] data = new byte[]{(byte) b};
    this.write(data, 0, 1);
  }

  @Override
  public void flush() {
    this.memoryContents.modified();
  }

  @Override
  public void close() {
    if (this.checker.close()) {
      this.checker.close();
      this.memoryContents.modified();
      this.memoryContents.closedStream(this.path, this.deleteOnClose);
    }
  }

}

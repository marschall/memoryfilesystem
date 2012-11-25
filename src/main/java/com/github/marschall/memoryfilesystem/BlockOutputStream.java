package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

abstract class BlockOutputStream extends OutputStream {

  final MemoryContents memoryContents;
  final ClosedStreamChecker checker;
  private final Path pathToDelete;


  BlockOutputStream(MemoryContents memoryContents, boolean deleteOnClose, Path path) {
    this.memoryContents = memoryContents;
    this.checker = new ClosedStreamChecker();
    if (deleteOnClose) {
      this.pathToDelete = path;
    } else {
      this.pathToDelete = null;
    }
  }

  @Override
  public void write(int b) throws IOException {
    byte[] data = new byte[]{(byte) b};
    this.write(data, 0, 1);
  }

  @Override
  public void flush() throws IOException {
    this.memoryContents.modified();
  }

  @Override
  public void close() throws IOException {
    this.checker.close();
    this.memoryContents.modified();
    this.memoryContents.closedStream(this.pathToDelete);
  }

}

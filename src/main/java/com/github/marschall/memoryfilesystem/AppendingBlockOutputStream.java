package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.Path;

class AppendingBlockOutputStream extends BlockOutputStream {

  AppendingBlockOutputStream(MemoryContents memoryContents, boolean deleteOnClose, Path path) {
    super(memoryContents, deleteOnClose, path);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    this.checker.check(this.path);
    this.memoryContents.writeAtEnd(b, off, len);
  }

}

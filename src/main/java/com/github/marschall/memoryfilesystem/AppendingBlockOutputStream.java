package com.github.marschall.memoryfilesystem;

import java.io.IOException;

public class AppendingBlockOutputStream extends BlockOutputStream {

  AppendingBlockOutputStream(MemoryContents memoryContents) {
    super(memoryContents);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    this.checker.check();
    this.memoryContents.writeAtEnd(b, off, len);
  }

}

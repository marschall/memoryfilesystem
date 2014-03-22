package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

final class NonAppendingBlockOutputStream extends BlockOutputStream {

  private final AtomicLong position;


  NonAppendingBlockOutputStream(MemoryContents memoryContents, boolean deleteOnClose, Path path) {
    super(memoryContents, deleteOnClose, path);
    this.position = new AtomicLong(0L);
  }


  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    this.checker.check(this.path);
    this.memoryContents.write(b, this.position.get(), off, len);
    this.position.getAndAdd(len);
  }

}

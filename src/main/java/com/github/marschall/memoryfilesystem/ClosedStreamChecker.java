package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Path;

final class ClosedStreamChecker extends ClosedChecker {

  void check(Path path) throws IOException {
    if (!this.open.get()) {
      throw new FileSystemException(path.toString(), null, "stream is closed");
    }
  }

}

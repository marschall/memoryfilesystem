package com.github.marschall.memoryfilesystem;

import java.io.IOException;

final class ClosedStreamChecker extends ClosedChecker {

  void check() throws IOException {
    if (!this.open) {
      throw new IOException("stream is closed");
    }
  }

}

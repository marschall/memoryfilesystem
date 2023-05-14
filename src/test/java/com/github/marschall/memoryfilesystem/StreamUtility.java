package com.github.marschall.memoryfilesystem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

final class StreamUtility {

  private StreamUtility() {
    throw new AssertionError("not instantiable");
  }

  static byte[] readFully(InputStream inputStream) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[8192];
    int read = inputStream.read(buffer);
    while (read != -1) {
      out.write(buffer, 0, read);
      read = inputStream.read(buffer);
    }
    return out.toByteArray();
  }

}

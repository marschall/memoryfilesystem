package com.github.marschall.memoryfilesystem;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

final class FileUtility {

  static void setContents(Path path, String contents) throws IOException {
    try (SeekableByteChannel channel = Files.newByteChannel(path, WRITE, TRUNCATE_EXISTING)) {
      channel.write(ByteBuffer.wrap(contents.getBytes(US_ASCII)));
    }
  }

  private FileUtility() {
    throw new AssertionError("not instantiable");
  }

}

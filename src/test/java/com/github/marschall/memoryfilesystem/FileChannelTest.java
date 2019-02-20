package com.github.marschall.memoryfilesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.NonWritableChannelException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class FileChannelTest {

  @RegisterExtension
  final FileSystemExtension extension = new FileSystemExtension();

  @Test
  void channelOnDirectoryReading() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path child1 = fileSystem.getPath("child1");
    Files.createFile(child1);

    FileChannel channel = FileChannel.open(child1, StandardOpenOption.READ);
    try {
      channel.force(true);
      assertTrue(channel.isOpen());
      assertEquals(0L, channel.position());
      assertEquals(0L, channel.size());
      channel.position(1L);
      assertEquals(1L, channel.position());
      assertThrows(NonWritableChannelException.class, () -> channel.lock());
    } finally {
      channel.close();
    }
    assertFalse(channel.isOpen());
  }

}

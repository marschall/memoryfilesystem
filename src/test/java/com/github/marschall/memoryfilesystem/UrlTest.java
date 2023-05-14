package com.github.marschall.memoryfilesystem;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class UrlTest {

  @RegisterExtension
  final FileSystemExtension extension = new FileSystemExtension();

  @Test
  void toUrl() throws IOException {
    Path root = this.extension.getFileSystem().getPath("/");
    Path file = root.resolve("test.txt");
    FileUtility.createAndSetContents(file, "abc");
    URL url = file.toUri().toURL();
    assertEquals("memory:name:///test.txt", url.toString());

    Path directory = Files.createDirectory(root.resolve("dir"));
    assertEquals("memory:name:///dir/", directory.toUri().toURL().toString());

    Path notExisting = root.resolve("notExisting");
    assertEquals("memory:name:///notExisting", notExisting.toUri().toURL().toString());
  }

  @Test
  void toUrlEquals() throws IOException {
    Path root = this.extension.getFileSystem().getPath("/");
    Path file = root.resolve("test.txt");
    FileUtility.createAndSetContents(file, "abc");
    URL url1 = file.toUri().toURL();
    URL url2 = file.toUri().toURL();
    assertNotSame(url1, url2);
    assertEquals(url1, url2);
  }

  @Test
  void openStream() throws IOException {
    Path root = this.extension.getFileSystem().getPath("/");
    String content = "abc";
    Path file = root.resolve("test.txt");
    FileUtility.createAndSetContents(file, content);

    URL url = new URL("memory:name:///test.txt");
    byte[] actual;
    try (InputStream stream = url.openStream()) {
      assertNotNull(stream);
      actual = StreamUtility.readFully(stream);
    }
    assertArrayEquals(content.getBytes(StandardCharsets.US_ASCII), actual);
  }

}

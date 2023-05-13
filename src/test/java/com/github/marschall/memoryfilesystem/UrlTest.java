package com.github.marschall.memoryfilesystem;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import com.github.marschall.memoryfilesystem.memory.MemoryURLStreamHandlerFactory;
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
  void urlInputStream() throws IOException {
    Path root = this.extension.getFileSystem().getPath("/");
    Path file = root.resolve("test.txt");
    FileUtility.createAndSetContents(file, "abc");
    URL url = new URL("memory:name:///test.txt");
    try (InputStream inputStream = url.openStream()) {
      assertNotNull(inputStream);
    }
  }

  @Test
  void urlOutputStream() throws IOException {
    Path root = this.extension.getFileSystem().getPath("/");
    Path file = root.resolve("test.txt");
    FileUtility.createAndSetContents(file, "abc");
    URL url = new URL("memory:name:///test.txt");
    try (InputStream inputStream = url.openStream()) {
      assertNotNull(inputStream);
    }
  }

  @Test
  void urlConnection() throws IOException {
    Path root = this.extension.getFileSystem().getPath("/");
    Path file = root.resolve("test.txt");
    FileUtility.createAndSetContents(file, "abc");

    URL url = new URL("memory:name:///test.txt");
    URLConnection urlConnection = url.openConnection();
    assertNotNull(urlConnection);

    assertEquals(3L, urlConnection.getContentLengthLong());
    assertThat(urlConnection.getLastModified(), greaterThan(0L));
  }

  @Test
  void existingFileUrl() throws IOException {
    Path path = Paths.get("src", "test", "java", "com", "github", "marschall", "memoryfilesystem", "UrlTest.java");
    URL url = path.toUri().toURL();
    URLConnection urlConnection = url.openConnection();

    assertThat(urlConnection.getContentLengthLong(), greaterThan(0L));
    assertThat(urlConnection.getLastModified(), greaterThan(0L));

    urlConnection.connect();
    assertThat(urlConnection.getContentLengthLong(), greaterThan(0L));
    assertThat(urlConnection.getLastModified(), greaterThan(0L));
  }

  @Test
  void notExistingFileUrl() throws IOException {
    Path path = Paths.get("src", "test", "java", "com", "github", "marschall", "memoryfilesystem", "UrlTest.class");
    URL url = path.toUri().toURL();
    URLConnection urlConnection = url.openConnection();

    assertEquals(0L, urlConnection.getContentLengthLong());
    assertEquals(0L, urlConnection.getLastModified());

    assertThrows(IOException.class, urlConnection::connect);
    assertThrows(IOException.class, url::openStream);
  }

  @Test
  void doNotSupportArbitraryProtocolsForHandlerFactory() {
    String someDefaultProtocol = "file";
    String someMissingProtocol = UUID.randomUUID().toString();
    MemoryURLStreamHandlerFactory factory = new MemoryURLStreamHandlerFactory();

    assertNull(factory.createURLStreamHandler(someDefaultProtocol));
    assertNull(factory.createURLStreamHandler(someMissingProtocol));
  }
}

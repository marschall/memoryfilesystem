package com.github.marschall.memoryfilesystem;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.marschall.memoryfilesystem.memory.MemoryURLStreamHandlerFactory;

class URLConnectionTest {

  @RegisterExtension
  final FileSystemExtension extension = new FileSystemExtension();
  
  @Test
  void getUrl() throws IOException {
    Path root = this.extension.getFileSystem().getPath("/");
    Path file = root.resolve("test.txt");
    FileUtility.createAndSetContents(file, "abc");
    URL url = new URL("memory:name:///test.txt");
    URLConnection connection = url.openConnection();
    assertSame(url, connection.getURL());
  }

  @Test
  void urlInputStream() throws IOException {
    Path root = this.extension.getFileSystem().getPath("/");
    String content = "abc";
    Path file = root.resolve("test.txt");
    FileUtility.createAndSetContents(file, content);

    URL url = new URL("memory:name:///test.txt");
    URLConnection connection = url.openConnection();
    byte[] actual;
    try (InputStream stream = connection.getInputStream()) {
      assertNotNull(stream);
      actual = StreamUtility.readFully(stream);
    }
    assertArrayEquals(content.getBytes(US_ASCII), actual);
  }

  @Test
  void urlOutputStream() throws IOException {
    Path root = this.extension.getFileSystem().getPath("/");
    Path path = Files.createFile(root.resolve("test.txt"));
    URL url = path.toUri().toURL();
    byte[] content = "abc".getBytes(US_ASCII);

    URLConnection connection = url.openConnection();
    connection.setDoOutput(true);
    try (OutputStream outputStream = connection.getOutputStream()) {
      assertNotNull(outputStream);
      outputStream.write(content);
    }
    assertArrayEquals(content, Files.readAllBytes(path));
  }

  @Test
  void urlConnection() throws IOException {
    Path root = this.extension.getFileSystem().getPath("/");
    Path file = root.resolve("test.txt");
    FileUtility.createAndSetContents(file, "abc");
    Instant lastModified = OffsetDateTime.of(LocalDate.of(2023, Month.MAY, 14), LocalTime.of(18, 48, 13), ZoneOffset.UTC).toInstant();
    Files.setAttribute(file, "lastModifiedTime", FileTime.from(lastModified));

    URL url = new URL("memory:name:///test.txt");
    URLConnection urlConnection = url.openConnection();
    assertNotNull(urlConnection);

    assertEquals(3L, urlConnection.getContentLengthLong());
    assertEquals(lastModified.toEpochMilli(), urlConnection.getLastModified());
    assertEquals("text/plain", urlConnection.getContentType());
    Map<String, List<String>> headerFields = urlConnection.getHeaderFields();
    assertNotNull(headerFields);
    Map<String, List<String>> expectedHeaders = new HashMap<>(4);
    expectedHeaders.put("last-modified", Collections.singletonList("Sun, 14 May 2023 18:48:13 GMT"));
    expectedHeaders.put("content-length", Collections.singletonList("3"));
    expectedHeaders.put("content-type", Collections.singletonList("text/plain"));
  }

  @Test
  void getContent() throws IOException {
    Path root = this.extension.getFileSystem().getPath("/");
    Path file = root.resolve("test.txt");
    String content = "abc";
    FileUtility.createAndSetContents(file, content);
    Instant lastModified = OffsetDateTime.of(LocalDate.of(2023, Month.MAY, 14), LocalTime.of(18, 48, 13), ZoneOffset.UTC).toInstant();
    Files.setAttribute(file, "lastModifiedTime", FileTime.from(lastModified));

    URL url = new URL("memory:name:///test.txt");
    URLConnection urlConnection = url.openConnection();
    assertNotNull(urlConnection);

    Object actualContent = urlConnection.getContent();
    assertThat(actualContent, CoreMatchers.instanceOf(InputStream.class));
    try (InputStream inputStream = (InputStream) actualContent) {
      assertArrayEquals(content.getBytes(US_ASCII), StreamUtility.readFully(inputStream));
    }
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

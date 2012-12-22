package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.FileExistsMatcher.exists;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Ignore;
import org.junit.Test;

@Ignore("broken")
public class DirectoriesTest {

  @Test
  public void basicCopy() throws IOException {
    try (FileSystem sourceFileSystem = MemoryFileSystemBuilder.newEmpty().build("source")) {
      try (FileSystem targetFileSystem = MemoryFileSystemBuilder.newEmpty().build("target")) {

        Files.createDirectory(sourceFileSystem.getPath("sub"));
        this.createFileWithContents(sourceFileSystem.getPath("aaa"), "bbb");
        this.createFileWithContents(sourceFileSystem.getPath("sub/ccc"), "ddd");

        Directories.copyRecursive(sourceFileSystem.getPath("/"), targetFileSystem.getPath("/"));

        assertThat(targetFileSystem.getPath("aaa"), exists());
        assertContents(targetFileSystem.getPath("aaa"), "bbb");

        assertThat(targetFileSystem.getPath("sub/ccc"), exists());
        assertContents(targetFileSystem.getPath("sub/ccc"), "ddd");
      }
    }
  }

  private void createFileWithContents(Path path, String contents) throws IOException {
    Files.createFile(path);
    this.setContents(path, contents);
  }

  private void setContents(Path path, String contents) throws IOException {
    try (SeekableByteChannel channel = Files.newByteChannel(path, WRITE, TRUNCATE_EXISTING)) {
      channel.write(ByteBuffer.wrap(contents.getBytes(US_ASCII)));
    }
  }

  private static void assertContents(Path path, String expected) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(expected.length());
    try (InputStream input = Files.newInputStream(path, READ)) {
      int read;
      byte[] buffer = new byte[512];
      while ((read = input.read(buffer)) != -1) {
        outputStream.write(buffer, 0, read);
      }
    }
    assertEquals(expected, new String(outputStream.toByteArray(), US_ASCII));
  }

}

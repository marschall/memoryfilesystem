package com.github.marschall.memoryfilesystem;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class BlockInputStreamTest {

  @RegisterExtension
  public final FileSystemExtension rule = new FileSystemExtension();

  @Test
  public void skipMoreThanAvailable() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

    Path path = fileSystem.getPath("file.txt");
    Files.createFile(path);
    Files.write(path, new byte[1]);

    try (InputStream inputStream = Files.newInputStream(path)) {
      long skipped = inputStream.skip(10);

      assertEquals(skipped, 1);
      assertEquals(inputStream.available(), 0);
      assertEquals(-1, inputStream.read());
    }
  }

  @Test
  public void skipMoreSmallNumber() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

    Path path = fileSystem.getPath("file.txt");
    Files.createFile(path);
    Files.write(path, new byte[3]);

    try (InputStream inputStream = Files.newInputStream(path)) {
      long skipped = inputStream.skip(2);

      assertEquals(skipped, 2);
      assertEquals(inputStream.available(), 1);
    }
  }

  @Test
  public void skipMoreLargeNumberNumber() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

    Path path = fileSystem.getPath("file.txt");
    Files.createFile(path);
    Files.write(path, new byte[2049]);

    try (InputStream inputStream = Files.newInputStream(path)) {
      long skipped = inputStream.skip(3000);

      assertEquals(skipped, 2048);
      assertEquals(inputStream.available(), 1);
    }
  }

  @Test
  public void transferTo() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

    Path path = fileSystem.getPath("file.txt");
    int size = MemoryInode.BLOCK_SIZE + 2;
    Files.createFile(path);
    fill(path, size);

    try (InputStream inputStream = Files.newInputStream(path)) {
      assertEquals(1L, inputStream.skip(1L));

      byte[] written;
      try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
        assertEquals(size - 1, ((BlockInputStream) inputStream).transferTo​(bos));
        written = bos.toByteArray();
      }
      assertEquals(size - 1, written.length); // hamcrest has no support for primitive arrays

      byte[] start = Arrays.copyOfRange(written, 0, 5);
      assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, start);

      byte[] expectedEnd = new byte[5];
      byte[] end = Arrays.copyOfRange(written, size - expectedEnd.length + 1 - 2, size + 1 - 2);
      byte expectedLast = (byte) (size % 256);
      for (int i = 0; i < expectedEnd.length; i++) {
        expectedEnd[expectedEnd.length - 1 - i] = (byte) (expectedLast - 1 - i);
      }
      assertArrayEquals(expectedEnd, end);

      assertEquals(0, inputStream.available());
      assertEquals(-1, inputStream.read());
    }

  }

  private static void fill(Path path, int length) throws IOException {
    byte[] data = new byte[256];
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) i;
    }
    try (OutputStream stream = Files.newOutputStream(path)) {
      int fullWrites = length / data.length;
      for (int i = 0; i < fullWrites; i++) {
        stream.write(data, 0, data.length);
      }
      int left = length % data.length;
      stream.write(data, 0, left);
    }
    assertEquals(length, Files.size(path));
  }

}

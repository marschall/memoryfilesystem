package com.github.marschall.memoryfilesystem;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;

public class BlockInputStreamTest {

  @Rule
  public final FileSystemRule rule = new FileSystemRule();

  @Test
  public void skipMoreThanAvailable() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

    Path path = fileSystem.getPath("file.txt");
    Files.createFile(path);
    Files.write(path, new byte[1]);

    InputStream inputStream = Files.newInputStream(path);
    long skipped = inputStream.skip(10);

    assertEquals(skipped, 1);
    assertEquals(inputStream.available(), 0);
  }

  @Test
  public void skipMoreSmallNumber() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

    Path path = fileSystem.getPath("file.txt");
    Files.createFile(path);
    Files.write(path, new byte[3]);

    InputStream inputStream = Files.newInputStream(path);
    long skipped = inputStream.skip(2);

    assertEquals(skipped, 2);
    assertEquals(inputStream.available(), 1);
  }

  @Test
  public void skipMoreLargeNumberNumber() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

    Path path = fileSystem.getPath("file.txt");
    Files.createFile(path);
    Files.write(path, new byte[2049]);

    InputStream inputStream = Files.newInputStream(path);
    long skipped = inputStream.skip(3000);

    assertEquals(skipped, 2048);
    assertEquals(inputStream.available(), 1);
  }

}

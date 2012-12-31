package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.FileContentsMatcher.hasContents;
import static com.github.marschall.memoryfilesystem.FileExistsMatcher.exists;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

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
        assertThat(targetFileSystem.getPath("aaa"), hasContents("bbb"));

        assertThat(targetFileSystem.getPath("sub/ccc"), exists());
        assertThat(targetFileSystem.getPath("sub/ccc"), hasContents("ddd"));
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

}

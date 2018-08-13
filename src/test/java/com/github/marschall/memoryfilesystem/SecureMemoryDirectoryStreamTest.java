package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.FileExistsMatcher.exists;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.ClosedDirectoryStreamException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardOpenOption;
import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;

public class SecureMemoryDirectoryStreamTest {

  @Rule
  public final FileSystemRule rule = new FileSystemRule();

  @Test
  public void deleteFileRelative() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

    Path originalFolder = fileSystem.getPath("original-folder");
    Files.createDirectory(originalFolder);
    Files.createFile(originalFolder.resolve("child"));
    Files.createFile(fileSystem.getPath("child"));

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(originalFolder)) {
      assumeTrue(directoryStream instanceof SecureDirectoryStream);
      SecureDirectoryStream<Path> secure = (SecureDirectoryStream<Path>) directoryStream;

      secure.deleteFile(fileSystem.getPath("child"));

      assertThat(fileSystem.getPath("child"), exists());
      assertThat(originalFolder.resolve("child"), not(exists()));
    }

  }

  @Test
  public void deleteFileAbsolute() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

    Path originalFolder = fileSystem.getPath("original-folder");
    Files.createDirectory(originalFolder);
    Files.createFile(originalFolder.resolve("child"));
    Files.createFile(fileSystem.getPath("child"));

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(originalFolder)) {
      assumeTrue(directoryStream instanceof SecureDirectoryStream);
      SecureDirectoryStream<Path> secure = (SecureDirectoryStream<Path>) directoryStream;

      secure.deleteFile(fileSystem.getPath("child").toAbsolutePath());

      assertThat(fileSystem.getPath("child"), not(exists()));
      assertThat(originalFolder.resolve("child"), exists());
    }

  }

  @Test
  public void deleteFileClosed() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

    Path originalFolder = fileSystem.getPath("original-folder");
    Files.createDirectory(originalFolder);

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(originalFolder)) {
      assumeTrue(directoryStream instanceof SecureDirectoryStream);
      SecureDirectoryStream<Path> secure = (SecureDirectoryStream<Path>) directoryStream;

      try {
        secure.deleteFile(fileSystem.getPath("/"));
        fail("closed secure directory stream should throw");
      } catch (ClosedDirectoryStreamException e) {
        // should reach here
      }

    }

  }

  @Test
  public void deleteDirectoryRelative() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

    Path originalFolder = fileSystem.getPath("original-folder");
    Files.createDirectory(originalFolder);
    Files.createDirectory(originalFolder.resolve("child"));
    Files.createDirectory(fileSystem.getPath("child"));

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(originalFolder)) {
      assumeTrue(directoryStream instanceof SecureDirectoryStream);
      SecureDirectoryStream<Path> secure = (SecureDirectoryStream<Path>) directoryStream;

      secure.deleteDirectory(fileSystem.getPath("child"));

      assertThat(fileSystem.getPath("child"), exists());
      assertThat(originalFolder.resolve("child"), not(exists()));
    }

  }

  @Test
  public void deleteDirectoryAbsolute() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

    Path originalFolder = fileSystem.getPath("original-folder");
    Files.createDirectory(originalFolder);
    Files.createDirectory(originalFolder.resolve("child"));
    Files.createDirectory(fileSystem.getPath("child"));

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(originalFolder)) {
      assumeTrue(directoryStream instanceof SecureDirectoryStream);
      SecureDirectoryStream<Path> secure = (SecureDirectoryStream<Path>) directoryStream;

      secure.deleteDirectory(fileSystem.getPath("child").toAbsolutePath());

      assertThat(fileSystem.getPath("child"), not(exists()));
      assertThat(originalFolder.resolve("child"), exists());
    }

  }

  @Test
  public void deleteDirectoryClosed() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

    Path originalFolder = fileSystem.getPath("original-folder");
    Files.createDirectory(originalFolder);

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(originalFolder)) {
      assumeTrue(directoryStream instanceof SecureDirectoryStream);
      SecureDirectoryStream<Path> secure = (SecureDirectoryStream<Path>) directoryStream;

      try {
        secure.deleteDirectory(fileSystem.getPath("/"));
        fail("closed secure directory stream should throw");
      } catch (ClosedDirectoryStreamException e) {
        // should reach here
      }

    }

  }

  @Test
  public void newByteChannelRelative() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

    Path originalFolder = fileSystem.getPath("original-folder");
    Files.createDirectory(originalFolder);

    Path relativePath = originalFolder.resolve("child");
    Files.createFile(relativePath);
    FileUtility.setContents(relativePath, "relative");

    Path absolutePath = fileSystem.getPath("child").toAbsolutePath();
    Files.createFile(absolutePath);
    FileUtility.setContents(absolutePath, "absolute");

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(originalFolder)) {
      assumeTrue(directoryStream instanceof SecureDirectoryStream);
      SecureDirectoryStream<Path> secure = (SecureDirectoryStream<Path>) directoryStream;

      try (SeekableByteChannel channel = secure.newByteChannel(absolutePath, Collections.singleton(StandardOpenOption.READ))) {

      }

    }

  }

}

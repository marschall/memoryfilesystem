package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.IsAbsoluteMatcher.isAbsolute;
import static com.github.marschall.memoryfilesystem.IsAbsoluteMatcher.isRelative;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("not portable")
class FileSystemCompatibility {

  @Test
  void forbidden() throws IOException {
    for (int i = 0; i < 128; ++i) {
      char c = (char) i;
      if (c != '/') {
        try {
          Path path = Paths.get(c + ".txt");
          Files.createFile(path);
          Files.delete(path);
        } catch (InvalidPathException e) {
          System.out.println("forbidden: " + Integer.toHexString(c));
        }
      }
    }
  }

  @Test
  void posixAttributes() throws IOException {
    FileSystem fileSystem = FileSystems.getDefault();
    Path path = fileSystem.getPath("/Users/marschall/Documents");
    if (Files.exists(path)) {
      System.out.println(Files.readAttributes(path, "posix:*"));
    }
  }

  @Test
  void iterator() {
    FileSystem fileSystem = FileSystems.getDefault();
    Path path = fileSystem.getPath("/Users/marschall/Documents");
    for (Path next : path) {
      assertThat(next, isRelative());
      assertThat(next, isRelative());
    }
  }

  @Test
  void iterable() {
    FileSystem fileSystem = FileSystems.getDefault();
    Path path = fileSystem.getPath("/Users/marschall/Documents");
    for (Path element : path) {
      assertThat(element, isRelative());
    }
  }

  @Test
  void getFileName() {

    FileSystem fileSystem = FileSystems.getDefault();
    Path usrBin = fileSystem.getPath("/usr/bin");
    Path bin = fileSystem.getPath("bin");

    assertTrue(Files.isDirectory(usrBin));
    assertFalse(Files.isRegularFile(usrBin));

    Path fileName = usrBin.getFileName();
    assertEquals(fileName, bin);
    assertThat(fileName, isRelative());
  }

  @Test
  void relativePath() {
    FileSystem fileSystem = FileSystems.getDefault();
    Path path = fileSystem.getPath("Documents");
    assertThat(path, isRelative());
    assertNull(path.getRoot());
  }

  @Test
  void absolutePath() {
    FileSystem fileSystem = FileSystems.getDefault();
    Path path = fileSystem.getPath("/Documents");
    assertThat(path, isAbsolute());
    assertNotNull(path.getRoot());
  }


}

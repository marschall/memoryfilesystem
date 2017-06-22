package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.IsAbsoluteMatcher.isAbsolute;
import static com.github.marschall.memoryfilesystem.IsAbsoluteMatcher.isRelative;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import org.junit.Ignore;
import org.junit.Test;

@Ignore("not portable")
public class FileSystemCompatibility {

  @Test
  public void forbidden() throws IOException {
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
  public void t() throws IOException {
    FileSystem fileSystem = FileSystems.getDefault();
    Path path = fileSystem.getPath("/Users/marschall/Documents");
    if (Files.exists(path)) {
      System.out.println(Files.readAttributes(path, "posix:*"));
    }
  }

  @Test
  public void iterator() {
    FileSystem fileSystem = FileSystems.getDefault();
    Path path = fileSystem.getPath("/Users/marschall/Documents");
    Iterator<Path> iterator = path.iterator();
    while (iterator.hasNext()) {
      Path next = iterator.next();
      assertThat(next, isRelative());
    }
  }

  @Test
  public void getFileName() {

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
  public void relativePath() {
    FileSystem fileSystem = FileSystems.getDefault();
    Path path = fileSystem.getPath("Documents");
    assertThat(path, isRelative());
    assertNull(path.getRoot());
  }

  @Test
  public void absolutePath() {
    FileSystem fileSystem = FileSystems.getDefault();
    Path path = fileSystem.getPath("/Documents");
    assertThat(path, isAbsolute());
    assertNotNull(path.getRoot());
  }


}

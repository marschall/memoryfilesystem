package com.github.marschall.memoryfilesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import org.junit.Test;

//@Ignore("needs to be cross checked")
public class FileSystemComptiblity {

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
      assertFalse(next.isAbsolute());
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
    assertFalse(fileName.isAbsolute());
  }

  @Test
  public void relativePath() {
    FileSystem fileSystem = FileSystems.getDefault();
    Path path = fileSystem.getPath("Documents");
    assertFalse(path.isAbsolute());
    assertNull(path.getRoot());
  }

  @Test
  public void absolutePath() {
    FileSystem fileSystem = FileSystems.getDefault();
    Path path = fileSystem.getPath("/Documents");
    assertTrue(path.isAbsolute());
    assertNotNull(path.getRoot());
  }


}

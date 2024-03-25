package com.github.marschall.memoryfilesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class PathTests {

  @RegisterExtension
  final FileSystemExtension extension = new FileSystemExtension();

  @Test
  void subPath() throws IOException {
    try (FileSystem fileSystem = this.extension.getFileSystem()) {
      Path absolutePath = fileSystem.getPath("/parent/child.txt");
      assertEquals(fileSystem.getPath("parent/child.txt"), absolutePath.subpath(0, 2));
      assertEquals(fileSystem.getPath("parent"), absolutePath.subpath(0, 1));
      assertEquals(fileSystem.getPath("child.txt"), absolutePath.subpath(1, 2));

      IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> absolutePath.subpath(1, 1));
      assertEquals("beginIndex must be smaller than 1 but was 1", e.getMessage());

      e = assertThrows(IllegalArgumentException.class, () -> absolutePath.subpath(-1, 1));
      assertEquals("beginIndex must not be negative but was -1", e.getMessage());

      e = assertThrows(IllegalArgumentException.class, () -> absolutePath.subpath(0, 3));
      assertEquals("endIndex must be smaller than or equal to 2 but was 3", e.getMessage());

      e = assertThrows(IllegalArgumentException.class, () -> absolutePath.subpath(2, 1));
      assertEquals("beginIndex must be smaller than 2 but was 2", e.getMessage());
    }
  }

  @Test
  void endsWithString() throws IOException {
    try (FileSystem fileSystem = this.extension.getFileSystem()) {
      Path absolutePath = fileSystem.getPath("/parent/child.txt");
      assertTrue(absolutePath.endsWith("/parent/child.txt"));
      assertTrue(absolutePath.endsWith("parent/child.txt"));
      assertTrue(absolutePath.endsWith("child.txt"));
      assertTrue(absolutePath.endsWith("child.txt/"));
      assertFalse(absolutePath.endsWith("/child.txt"));
      assertFalse(absolutePath.endsWith("txt"));
    }
  }

}

package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.Constants.SAMPLE_ENV;
import static com.github.marschall.memoryfilesystem.Constants.SAMPLE_URI;
import static com.github.marschall.memoryfilesystem.IsAbsoluteMatcher.isAbsolute;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class RootTest {


  @Test
  void unixRoot() throws IOException {
    try (FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build("name")) {
      for (Path root : fileSystem.getRootDirectories()) {
        assertCommonRootProperties(root);

        assertEquals("/", root.toString());
        assertFalse(root.startsWith(""));
        assertFalse(root.startsWith("root"));
        assertTrue(root.startsWith("/"));
        assertTrue(root.endsWith("/"));
        assertFalse(root.endsWith(""));
        assertFalse(root.endsWith("root"));
      }
    }
  }

  private static void assertCommonRootProperties(Path root) {
    assertTrue(root.startsWith(root));
    assertTrue(root.endsWith(root));

    assertEquals(root, root.normalize());
    assertEquals(root, root.toAbsolutePath());

    assertThat(root, isAbsolute());
    assertEquals(root, root.getRoot());
    assertNull(root.getFileName());
    assertNull(root.getParent());
    assertEquals(0, root.getNameCount());
    assertFalse(root.iterator().hasNext());

    assertEquals(root.toAbsolutePath(), Paths.get(root.toUri()));

    for (int i = -1; i < 2; ++i) {
      int j = i;
      assertThrows(IllegalArgumentException.class,
              () -> root.getName(j),
              "root should not support #getName(int)");
    }

    assertThrows(NullPointerException.class,
            () -> root.endsWith((String) null),
            "path#endsWith(null) should not work");

    assertThrows(NullPointerException.class,
            () -> root.startsWith((String) null),
            "path#startsWith(null) should not work");

  }

  @Test
  void defaultRoot() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      for (Path root : fileSystem.getRootDirectories()) {
        assertCommonRootProperties(root);
      }
    }
  }

  @Test
  void windowsRootInvalid1() throws IOException {
    try (FileSystem fileSystem = MemoryFileSystemBuilder.newWindows().build("name")) {
      assertThrows(InvalidPathException.class, () -> fileSystem.getPath("/C:\\"));
    }
  }

  @Test
  void windowsRootInvalid2() throws IOException {
    try (FileSystem fileSystem = MemoryFileSystemBuilder.newWindows().build("name")) {
      assertThrows(InvalidPathException.class, () -> fileSystem.getPath("\\C:\\"));
    }
  }

  @Test
  void windowsPaths() throws IOException {
    try (FileSystem fileSystem = MemoryFileSystemBuilder.newWindows().build("name")) {
      fileSystem.getPath("C:\\");

      assertEquals("C:\\temp", fileSystem.getPath("C:/temp").toString());
      assertEquals("C:\\temp", fileSystem.getPath("C:/temp").toString());
    }

  }

  @Test
  void windowsRootMethods() throws IOException {
    try (FileSystem fileSystem = MemoryFileSystemBuilder.newWindows().addRoot("D:\\").build("name")) {
      List<Path> roots = this.asList(fileSystem.getRootDirectories());
      for (int i = 0; i < roots.size(); ++i) {
        Path currentRoot = roots.get(i);
        assertCommonRootProperties(currentRoot);

        // don't assume order
        assertThat(currentRoot.toString(), anyOf(equalTo("C:\\"), equalTo("D:\\")));
        assertFalse(currentRoot.startsWith(""));
        assertFalse(currentRoot.startsWith("/"));
        assertTrue(currentRoot.startsWith(currentRoot.toString()));

        assertFalse(currentRoot.endsWith(""));
        assertFalse(currentRoot.endsWith("/"));
        assertTrue(currentRoot.endsWith(currentRoot.toString()));

        for (int j = i + 1; j < roots.size(); ++j) {
          Path otherRoot = roots.get(j);
          assertFalse(currentRoot.startsWith(otherRoot));
          assertFalse(otherRoot.startsWith(currentRoot));

          assertFalse(currentRoot.endsWith(otherRoot));
          assertFalse(otherRoot.endsWith(currentRoot));

          assertFalse(currentRoot.endsWith(otherRoot.toString()));
        }
      }
    }
  }


  private <T> List<T> asList(Iterable<T> iterable) {
    List<T> result = new ArrayList<>();
    for (T each : iterable) {
      result.add(each);
    }
    return result;
  }

}

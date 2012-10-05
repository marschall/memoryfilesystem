package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.Constants.SAMPLE_ENV;
import static com.github.marschall.memoryfilesystem.Constants.SAMPLE_URI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class RootTest {


  @Test
  public void unixRoot() throws IOException {
    Map<String, ?> env = EnvironmentBuilder.newUnix().build();
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, env)) {
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

  private void assertCommonRootProperties(Path root) {
    assertTrue(root.startsWith(root));
    assertTrue(root.endsWith(root));

    assertEquals(root, root.normalize());
    assertEquals(root, root.toAbsolutePath());

    assertTrue(root.isAbsolute());
    assertEquals(root, root.getRoot());
    assertNull(root.getFileName());
    assertNull(root.getParent());
    assertEquals(0, root.getNameCount());
    assertFalse(root.iterator().hasNext());

    assertEquals(root.toAbsolutePath(), Paths.get(root.toUri()));

    for (int i = -1; i < 2; ++i) {
      try {
        root.getName(i);
        fail("root should not support #getName(int)");
      } catch (IllegalArgumentException e) {
        // should reach here
      }
    }

    try {
      root.endsWith((String) null);
      fail("path#endsWith(null) should not work");
    } catch (NullPointerException e) {
      // should reach here
    }
    try {
      root.startsWith((String) null);
      fail("path#startsWith(null) should not work");
    } catch (NullPointerException e) {
      // should reach here
    }

  }

  @Test
  public void defaultRoot() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      for (Path root : fileSystem.getRootDirectories()) {
        assertCommonRootProperties(root);
      }
    }
  }
  
  @Test(expected = InvalidPathException.class)
  public void windowsRootInvalid1() throws IOException {
    Map<String, ?> env = EnvironmentBuilder.newWindows().build();
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, env)) {
      fileSystem.getPath("/C:\\");
    }
  }
  
  @Test(expected = InvalidPathException.class)
  public void windowsRootInvalid2() throws IOException {
    Map<String, ?> env = EnvironmentBuilder.newWindows().build();
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, env)) {
      fileSystem.getPath("\\C:\\");
    }
  }
  
  @Test
  public void windowsPaths() throws IOException {
    Map<String, ?> env = EnvironmentBuilder.newWindows().build();
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, env)) {
      fileSystem.getPath("C:\\");
      
      assertEquals("C:\\temp", fileSystem.getPath("C:/temp").toString());
      assertEquals("C:\\temp", fileSystem.getPath("C:/temp").toString());
    }
    
  }

  @Test
  public void windowsRootMethods() throws IOException {
    Map<String, ?> env = EnvironmentBuilder.newWindows().addRoot("D:\\").build();
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, env)) {
      List<Path> roots = asList(fileSystem.getRootDirectories());
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

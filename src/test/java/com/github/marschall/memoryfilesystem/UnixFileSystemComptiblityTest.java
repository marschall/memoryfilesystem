package com.github.marschall.memoryfilesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class UnixFileSystemComptiblityTest {

  private final FileSystem fileSystem;

  public UnixFileSystemComptiblityTest(FileSystem fileSystem) {
    this.fileSystem = fileSystem;
  }

  @Test
  public void readPosixSize() throws IOException {
    Path path = this.fileSystem.getPath("/");
    Map<String, Object> attributes = Files.readAttributes(path, "posix:size");
    //TODO fix hamcrest
    //    assertThat(attributes, hasSize(1));
    assertEquals(1, attributes.size());
    assertEquals(Collections.singleton("size"), attributes.keySet());
    //TODO fix hamcrest
    //    assertThat(attributes.values().iterator().next(), isA(Long.class));
    assertTrue(attributes.values().iterator().next() instanceof Long);
  }

  @Test
  public void readPosixAttributeNames() throws IOException {
    Path path = this.fileSystem.getPath("/");
    Map<String, Object> attributes = Files.readAttributes(path, "posix:*");
    Set<String> expectedAttributeNames = new HashSet<>(Arrays.asList(
            "lastModifiedTime",
            "fileKey",
            "isDirectory",
            "lastAccessTime",
            "isOther",
            "isSymbolicLink",
            "owner",
            "permissions",
            "isRegularFile",
            "creationTime",
            "group",
            "size"));
    assertEquals(expectedAttributeNames, attributes.keySet());
  }

  @After
  public void tearDown() throws IOException {
    if (this.fileSystem instanceof MemoryFileSystem) {
      this.fileSystem.close();
    }
  }

  @Parameters
  public static List<Object[]> fileSystems() throws IOException {
    FileSystem defaultFileSystem = FileSystems.getDefault();
    FileSystem memoryFileSystem = MemoryFileSystemBuilder.newUnix().build("posix");
    boolean isPosix = defaultFileSystem.supportedFileAttributeViews().contains("posix");
    // TODO don't run on Max
    if (isPosix) {
      return Arrays.asList(new Object[]{memoryFileSystem},
              new Object[]{defaultFileSystem});
    } else {
      return Collections.singletonList(new Object[]{memoryFileSystem});
    }
  }

}

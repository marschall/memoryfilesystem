package com.github.marschall.memoryfilesystem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class UnixFileSystemComptiblityTest {

  @Rule
  public final UnixFileSystemRule rule = new UnixFileSystemRule();

  private FileSystem fileSystem;

  private final boolean useDefault;

  public UnixFileSystemComptiblityTest(boolean useDefault) {
    this.useDefault = useDefault;
  }

  FileSystem getFileSystem() {
    if (this.fileSystem == null) {
      if (this.useDefault) {
        this.fileSystem = FileSystems.getDefault();
      } else {
        this.fileSystem = this.rule.getFileSystem();
      }
    }
    return this.fileSystem;
  }

  @Test
  public void supportsOwner() {
    assertThat(this.getFileSystem().supportedFileAttributeViews(), hasItem("owner"));
  }

  @Test
  public void notExistingView() throws IOException {
    Path path = this.getFileSystem().getPath("/foo/bar/does/not/exist");
    BasicFileAttributeView attributeView = Files.getFileAttributeView(path, BasicFileAttributeView.class);
    assertNotNull(attributeView);
  }

  @Test
  public void readOwner() throws IOException {
    Path path = this.getFileSystem().getPath("/");
    Map<String, Object> attributes = Files.readAttributes(path, "owner:owner");
    //TODO fix hamcrest
    //assertThat(attributes, hasSize(1));
    assertEquals(1, attributes.size());
    assertEquals(Collections.singleton("owner"), attributes.keySet());
    //TODO fix hamcrest
    //assertThat(attributes.values().iterator().next(), isA(Long.class));
    Object value = attributes.values().iterator().next();
    assertNotNull(value);
    assertTrue(value instanceof UserPrincipal);
    assertFalse(value instanceof GroupPrincipal);
  }

  @Test
  public void readPosixSize() throws IOException {
    Path path = this.getFileSystem().getPath("/");
    Map<String, Object> attributes = Files.readAttributes(path, "posix:size");
    //TODO fix hamcrest
    //assertThat(attributes, hasSize(1));
    assertEquals(1, attributes.size());
    assertEquals(Collections.singleton("size"), attributes.keySet());
    //TODO fix hamcrest
    //assertThat(attributes.values().iterator().next(), isA(Long.class));
    assertTrue(attributes.values().iterator().next() instanceof Long);
  }

  @Test
  public void readPosixAttributeNames() throws IOException {
    Path path = this.getFileSystem().getPath("/");
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

  @Test
  public void readOwnerAttributeNames() throws IOException {
    Path path = this.getFileSystem().getPath("/");
    Map<String, Object> attributes = Files.readAttributes(path, "owner:*");
    Set<String> expectedAttributeNames = Collections.singleton("owner");
    assertEquals(expectedAttributeNames, attributes.keySet());
  }

  @Parameters
  public static List<Object[]> fileSystems() throws IOException {
    FileSystem defaultFileSystem = FileSystems.getDefault();
    boolean isPosix = defaultFileSystem.supportedFileAttributeViews().contains("posix");
    // TODO don't run on Mac
    if (isPosix) {
      return Arrays.asList(new Object[]{true},
              new Object[]{false});
    } else {
      return Collections.singletonList(new Object[]{false});
    }
  }

}

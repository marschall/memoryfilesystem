package com.github.marschall.memoryfilesystem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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


  @Parameters
  public static List<Object[]> fileSystems() throws IOException {
    FileSystem defaultFileSystem = FileSystems.getDefault();
    boolean isPosix = defaultFileSystem.supportedFileAttributeViews().contains("posix");
    String osName = (String) System.getProperties().get("os.name");
    boolean isMac = osName.startsWith("Mac");
    if (isPosix && !isMac) {
      return Arrays.asList(new Object[]{true},
              new Object[]{false});
    } else {
      return Collections.singletonList(new Object[]{false});
    }
  }

  @Test(expected = UnsupportedOperationException.class)
  public void initialLastModifiedTime() throws ParseException, IOException {
    this.assertUnsupportedCreateOption("lastAccessTime");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void initialCreationTime() throws ParseException, IOException {
    this.assertUnsupportedCreateOption("creationTime");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void initiallastModifiedTime() throws ParseException, IOException {
    this.assertUnsupportedCreateOption("lastModifiedTime");
  }

  private void assertUnsupportedCreateOption(String attributeName) throws IOException, ParseException {
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    FileTime time = FileTime.fromMillis(format.parse("2012-11-07T20:30:22").getTime());

    FileAttribute<?> lastModifiedAttribute = new StubFileAttribute<>(attributeName, time);

    Path path = this.getFileSystem().getPath("time");
    Files.createFile(path, lastModifiedAttribute);
    fail("'" + attributeName + "' not supported as initial attribute");
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

  private static void assertContents(Path p, byte[] contents) throws IOException {
    int expectedContentSize = contents.length;
    byte[] buffer = new byte[expectedContentSize + 1];
    try (InputStream input = Files.newInputStream(p)) {
      assertEquals(expectedContentSize, input.read(buffer, 0, expectedContentSize));
      assertEquals(-1, input.read(buffer, expectedContentSize, 1));
      for (int i = 0; i < expectedContentSize; i++) {
        assertEquals(contents[i], buffer[i]);
      }
    }
  }


  @Test
  public void outputStreamDontTruncate() throws IOException {
    Path path = Paths.get("output");
    try {
      Files.createFile(path);
      try (OutputStream output = Files.newOutputStream(path, StandardOpenOption.WRITE)) {
        output.write("11111".getBytes("US-ASCII"));
        output.flush();
      }
      try (OutputStream output = Files.newOutputStream(path, StandardOpenOption.WRITE)) {
        output.write("22".getBytes("US-ASCII"));
      }
      assertContents(path, "22111".getBytes("US-ASCII"));
    } finally {
      Files.deleteIfExists(path);
    }

  }

  @Test
  public void outputStreamAppend() throws IOException {
    Path path = Paths.get("output");
    try {
      Files.createFile(path);
      try (OutputStream output = Files.newOutputStream(path, StandardOpenOption.WRITE)) {
        output.write("11111".getBytes("US-ASCII"));
        output.flush();
      }
      try (OutputStream output = Files.newOutputStream(path, StandardOpenOption.APPEND)) {
        output.write("22".getBytes("US-ASCII"));
      }
      assertContents(path, "1111122".getBytes("US-ASCII"));
    } finally {
      Files.deleteIfExists(path);
    }

  }


  @Test
  public void outputStreamTruncateByDefault() throws IOException {
    Path path = Paths.get("output");
    try {
      Files.createFile(path);
      try (OutputStream output = Files.newOutputStream(path)) {
        output.write("11111".getBytes("US-ASCII"));
        output.flush();
      }
      try (OutputStream output = Files.newOutputStream(path)) {
        output.write("22".getBytes("US-ASCII"));
      }
      assertContents(path, "22".getBytes("US-ASCII"));
    } finally {
      Files.deleteIfExists(path);
    }

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

}

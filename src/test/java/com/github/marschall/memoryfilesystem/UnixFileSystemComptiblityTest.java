package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.FileContentsMatcher.hasContents;
import static com.github.marschall.memoryfilesystem.FileExistsMatcher.exists;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.text.Normalizer;
import java.text.Normalizer.Form;
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
  public final PosixFileSystemRule rule = new PosixFileSystemRule();

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


  @Parameters(name = "navite: {0}")
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


  @Test
  public void forbiddenCharacters() throws IOException {
    try {
      char c = 0;
      this.getFileSystem().getPath(c + ".txt");
      fail("0x00 should be forbidden");
    } catch (InvalidPathException e) {
      // should reach here
    }
  }

  @Test
  public void notForbiddenCharacters() throws IOException {
    for (int i = 1; i < 128; ++i) {
      char c = (char) i;
      if (c != '/') {
        Path path = this.getFileSystem().getPath(c + ".txt");
        assertNotNull(path);
        try {
          Files.createFile(path);
        } finally {
          Files.delete(path);
        }
      }
    }
  }

  @Test
  public void isHidden() throws IOException {
    Path hidden = this.getFileSystem().getPath(".hidden");
    Files.createFile(hidden);
    try {
      assertTrue(Files.isHidden(hidden));
    } finally {
      Files.delete(hidden);
    }
  }

  @Test
  public void isNotHidden() throws IOException {
    Path hidden = this.getFileSystem().getPath("not_hidden");
    Files.createFile(hidden);
    try {
      assertFalse(Files.isHidden(hidden));
    } finally {
      Files.delete(hidden);
    }
  }

  @Test
  public void rootAttributes() throws IOException {
    Path root = this.getFileSystem().getRootDirectories().iterator().next();
    BasicFileAttributes attributes = Files.readAttributes(root, BasicFileAttributes.class);
    assertTrue(attributes.isDirectory());
    assertFalse(attributes.isRegularFile());
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
      assertThat(path, hasContents("22111"));
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
      assertThat(path, hasContents("1111122"));
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
      assertThat(path, hasContents("22"));
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

  @Test
  public void startsWith() {
    FileSystem fileSystem = this.getFileSystem();

    assertTrue(fileSystem.getPath("a").startsWith(fileSystem.getPath("a")));
    assertFalse(fileSystem.getPath("a").startsWith(fileSystem.getPath("/a")));
    assertFalse(fileSystem.getPath("a").startsWith(fileSystem.getPath("/")));
    assertFalse(fileSystem.getPath("a").startsWith(fileSystem.getPath("")));

    assertTrue(fileSystem.getPath("/a").startsWith(fileSystem.getPath("/a")));
    assertFalse(fileSystem.getPath("/a").startsWith(fileSystem.getPath("a")));
    assertTrue(fileSystem.getPath("/a").startsWith(fileSystem.getPath("/")));
    assertFalse(fileSystem.getPath("/a").startsWith(fileSystem.getPath("")));

    assertTrue(fileSystem.getPath("/").startsWith(fileSystem.getPath("/")));
    assertFalse(fileSystem.getPath("/").startsWith(fileSystem.getPath("")));
    assertFalse(fileSystem.getPath("/").startsWith(fileSystem.getPath("a")));
    assertFalse(fileSystem.getPath("/").startsWith(fileSystem.getPath("/a")));
    assertFalse(fileSystem.getPath("/").startsWith(fileSystem.getPath("a/b")));
    assertFalse(fileSystem.getPath("/").startsWith(fileSystem.getPath("/a/b")));

    assertFalse(fileSystem.getPath("").startsWith(fileSystem.getPath("/")));
    assertTrue(fileSystem.getPath("").startsWith(fileSystem.getPath("")));
    assertFalse(fileSystem.getPath("").startsWith(fileSystem.getPath("a")));
    assertFalse(fileSystem.getPath("").startsWith(fileSystem.getPath("/a")));
    assertFalse(fileSystem.getPath("").startsWith(fileSystem.getPath("a/b")));
    assertFalse(fileSystem.getPath("").startsWith(fileSystem.getPath("/a/b")));
  }

  @Test
  public void endsWith() {
    FileSystem fileSystem = this.getFileSystem();

    assertTrue(fileSystem.getPath("a").endsWith(fileSystem.getPath("a")));
    assertFalse(fileSystem.getPath("a").endsWith(fileSystem.getPath("a/b")));
    assertFalse(fileSystem.getPath("a").endsWith(fileSystem.getPath("/a")));
    assertFalse(fileSystem.getPath("a").endsWith(fileSystem.getPath("/")));
    assertFalse(fileSystem.getPath("a").endsWith(fileSystem.getPath("")));
    assertFalse(fileSystem.getPath("a/b").endsWith(fileSystem.getPath("a")));
    assertTrue(fileSystem.getPath("a/b").endsWith(fileSystem.getPath("b")));
    assertTrue(fileSystem.getPath("a/b").endsWith(fileSystem.getPath("a/b")));
    assertFalse(fileSystem.getPath("a/b").endsWith(fileSystem.getPath("/a")));
    assertFalse(fileSystem.getPath("a/b").endsWith(fileSystem.getPath("/")));
    assertFalse(fileSystem.getPath("a/b").endsWith(fileSystem.getPath("")));

    assertTrue(fileSystem.getPath("/a").endsWith(fileSystem.getPath("/a")));
    assertFalse(fileSystem.getPath("/a").endsWith(fileSystem.getPath("/a/b")));
    assertTrue(fileSystem.getPath("/a").endsWith(fileSystem.getPath("a")));
    assertFalse(fileSystem.getPath("/a").endsWith(fileSystem.getPath("/")));
    assertFalse(fileSystem.getPath("/a").endsWith(fileSystem.getPath("")));
    assertFalse(fileSystem.getPath("/a/b").endsWith(fileSystem.getPath("/a")));
    assertTrue(fileSystem.getPath("/a/b").endsWith(fileSystem.getPath("/a/b")));
    assertFalse(fileSystem.getPath("/a/b").endsWith(fileSystem.getPath("/b")));
    assertTrue(fileSystem.getPath("/a/b").endsWith(fileSystem.getPath("b")));
    assertFalse(fileSystem.getPath("/a/b").endsWith(fileSystem.getPath("/a/b/c")));
    assertFalse(fileSystem.getPath("/a/b").endsWith(fileSystem.getPath("a")));
    assertFalse(fileSystem.getPath("/a/b").endsWith(fileSystem.getPath("/")));
    assertFalse(fileSystem.getPath("/a/b").endsWith(fileSystem.getPath("")));

    assertTrue(fileSystem.getPath("/").endsWith(fileSystem.getPath("/")));
    assertFalse(fileSystem.getPath("/").endsWith(fileSystem.getPath("")));
    assertFalse(fileSystem.getPath("/").endsWith(fileSystem.getPath("a")));
    assertFalse(fileSystem.getPath("/").endsWith(fileSystem.getPath("/a")));
    assertFalse(fileSystem.getPath("/").endsWith(fileSystem.getPath("a/b")));
    assertFalse(fileSystem.getPath("/").endsWith(fileSystem.getPath("/a/b")));

    assertFalse(fileSystem.getPath("").endsWith(fileSystem.getPath("/")));
    assertTrue(fileSystem.getPath("").endsWith(fileSystem.getPath("")));
    assertFalse(fileSystem.getPath("").endsWith(fileSystem.getPath("a")));
    assertFalse(fileSystem.getPath("").endsWith(fileSystem.getPath("/a")));
    assertFalse(fileSystem.getPath("").endsWith(fileSystem.getPath("a/b")));
    assertFalse(fileSystem.getPath("").endsWith(fileSystem.getPath("/a/b")));
  }

  @Test
  public void resolve() {
    FileSystem fileSystem = this.getFileSystem();

    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("/a").resolve(fileSystem.getPath("/")));
  }


  @Test(expected = IllegalArgumentException.class)
  public void slash() {
    FileSystem fileSystem = this.getFileSystem();
    Path path = fileSystem.getPath("/");
    path.subpath(0, 1);
  }

  @Test
  public void test() {
    FileSystem fileSystem = this.getFileSystem();
    Iterable<Path> rootDirectories = fileSystem.getRootDirectories();
    Path root = rootDirectories.iterator().next();
    assertTrue(root.endsWith(root));
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
    assertTrue(root.startsWith(root));
    assertFalse(root.startsWith(""));
    assertTrue(root.startsWith("/"));
    assertTrue(root.endsWith("/"));
    assertFalse(root.endsWith(""));
  }


  @Test
  public void aboluteGetParent() {
    FileSystem fileSystem = this.getFileSystem();

    Path usrBin = fileSystem.getPath("/usr/bin");
    Path usr = fileSystem.getPath("/usr");

    assertEquals(usr, usrBin.getParent());
    Path root = fileSystem.getRootDirectories().iterator().next();
    assertEquals(root, usr.getParent());

    assertEquals(fileSystem.getPath("usr/bin/a"), fileSystem.getPath("usr/bin").resolve(fileSystem.getPath("a")));

    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("/../../..").normalize());
    assertEquals(fileSystem.getPath("/a/b"), fileSystem.getPath("/../a/b").normalize());
    assertEquals(fileSystem.getPath("../../.."), fileSystem.getPath("../../..").normalize());
    assertEquals(fileSystem.getPath("../../.."), fileSystem.getPath(".././../..").normalize());
    assertEquals(fileSystem.getPath("../../a/b/c"), fileSystem.getPath("../../a/b/c").normalize());
    assertEquals(fileSystem.getPath("a"), fileSystem.getPath("a/b/..").normalize());
    assertEquals(fileSystem.getPath(""), fileSystem.getPath("a/..").normalize());
    assertEquals(fileSystem.getPath(".."), fileSystem.getPath("..").normalize());
    //    System.out.println(fileSystem.getPath("a").subpath(0, 0)); // throws excepption
    assertEquals(fileSystem.getPath("a"), fileSystem.getPath("/a/b").getName(0));
    assertEquals(fileSystem.getPath("b"), fileSystem.getPath("/a/b").getName(1));
    assertEquals(fileSystem.getPath("a"), fileSystem.getPath("a/b").getName(0));
    assertEquals(fileSystem.getPath("b"), fileSystem.getPath("a/b").getName(1));
  }

  @Test
  public void pathOrdering() {
    FileSystem fileSystem = this.getFileSystem();
    Path root = fileSystem.getPath("/");
    Path empty = fileSystem.getPath("");
    Path a = fileSystem.getPath("a");
    Path slashA = fileSystem.getPath("/a");
    Path slashAA = fileSystem.getPath("/a/a");

    assertTrue(root.compareTo(a) < 0);
    assertTrue(root.compareTo(slashA) < 0);
    assertTrue(a.compareTo(slashA) > 0);
    assertThat(a, greaterThan(slashA));
    assertThat(slashA, lessThan(a));
    assertTrue(slashA.compareTo(slashAA) < 0);

    assertThat(a, greaterThan(empty));
  }

  @Test
  public void relativeGetParent() {
    FileSystem fileSystem = this.getFileSystem();
    Path usrBin = fileSystem.getPath("usr/bin");
    Path usr = fileSystem.getPath("usr");

    assertEquals(usr, usrBin.getParent());
    assertNull(usr.getParent());
  }

  @Test
  public void resolveSibling() {
    FileSystem fileSystem = this.getFileSystem();

    assertEquals(fileSystem.getPath("a"), fileSystem.getPath("/").resolveSibling(fileSystem.getPath("a")));
    assertEquals(fileSystem.getPath("b"), fileSystem.getPath("a").resolveSibling(fileSystem.getPath("b")));
    assertEquals(fileSystem.getPath(""), fileSystem.getPath("/").resolveSibling(fileSystem.getPath("")));

    assertEquals(fileSystem.getPath("/c/d"), fileSystem.getPath("/a/b").resolveSibling(fileSystem.getPath("/c/d")));
    assertEquals(fileSystem.getPath("/c/d"), fileSystem.getPath("a/b").resolveSibling(fileSystem.getPath("/c/d")));

    assertEquals(fileSystem.getPath(""), fileSystem.getPath("a").resolveSibling(fileSystem.getPath("")));
    assertEquals(fileSystem.getPath("/a"), fileSystem.getPath("/a/b").resolveSibling(fileSystem.getPath("")));
    assertEquals(fileSystem.getPath("a"), fileSystem.getPath("a/b").resolveSibling(fileSystem.getPath("")));
    assertEquals(fileSystem.getPath("a/b"), fileSystem.getPath("").resolveSibling(fileSystem.getPath("a/b")));
    assertEquals(fileSystem.getPath("/a/b"), fileSystem.getPath("").resolveSibling(fileSystem.getPath("/a/b")));

    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("a/b").resolveSibling(fileSystem.getPath("/")));
    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("/a/b").resolveSibling(fileSystem.getPath("/")));
    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("").resolveSibling(fileSystem.getPath("/")));

    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("/a").resolveSibling(fileSystem.getPath("")));
    assertEquals(fileSystem.getPath(""), fileSystem.getPath("a").resolveSibling(fileSystem.getPath("")));
  }

  @Test
  public void unixNoNormalization() throws IOException {
    /*
     * Verifies that Linux does no Unicode normalization and that we can have
     * both a NFC and NFD file.
     */
    // https://github.com/marschall/memoryfilesystem/pull/51
    assumeTrue(Charset.defaultCharset().equals(UTF_8));
    FileSystem fileSystem = this.getFileSystem();
    String aUmlaut = "\u00C4";
    Path nfcPath = fileSystem.getPath(aUmlaut);
    String normalized = Normalizer.normalize(aUmlaut, Form.NFD);
    Path nfdPath = fileSystem.getPath(normalized);

    Path nfcFile = null;
    Path nfdFile = null;
    try {
      nfcFile = Files.createFile(nfcPath);
      assertEquals(1, nfcFile.getFileName().toString().length());
      assertEquals(1, nfcFile.toAbsolutePath().getFileName().toString().length());
      assertEquals(1, nfcFile.toRealPath().getFileName().toString().length());

      assertThat(nfcPath, exists());
      assertThat(nfdPath, not(exists()));

      nfdFile = Files.createFile(nfdPath);
      assertEquals(2, nfdFile.getFileName().toString().length());
      assertEquals(2, nfdFile.toAbsolutePath().getFileName().toString().length());
      assertEquals(2, nfdFile.toRealPath().getFileName().toString().length());

      assertThat(nfcPath, not(equalTo(nfdPath)));
      assertFalse(Files.isSameFile(nfcPath, nfdPath));
      assertFalse(Files.isSameFile(nfdPath, nfcPath));
    } finally {
      if (nfcFile != null) {
        Files.delete(nfcFile);
      }
      if (nfdFile != null) {
        Files.delete(nfdFile);
      }
    }
  }

  @Test
  public void unixPaths() throws IOException {

    // https://github.com/marschall/memoryfilesystem/pull/51
    assumeTrue(Charset.defaultCharset().equals(UTF_8));
    FileSystem fileSystem = this.getFileSystem();
    String aUmlaut = "\u00C4";
    String normalized = Normalizer.normalize(aUmlaut, Form.NFD);
    assertEquals(1, aUmlaut.length());
    assertEquals(2, normalized.length());
    Path aPath = fileSystem.getPath("/" + aUmlaut);
    Path nPath = fileSystem.getPath("/" + normalized);
    assertEquals(1, aPath.getName(0).toString().length());
    // verify a NFC path is not equal to a NFD path
    assertThat(aPath, not(equalTo(nPath)));
  }

}

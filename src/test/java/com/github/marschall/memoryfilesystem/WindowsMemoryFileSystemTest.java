package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.FileExistsMatcher.exists;
import static com.github.marschall.memoryfilesystem.IsAbsoluteMatcher.isAbsolute;
import static com.github.marschall.memoryfilesystem.IsAbsoluteMatcher.isRelative;
import static com.github.marschall.memoryfilesystem.IsHiddenMatcher.isHidden;
import static com.github.marschall.memoryfilesystem.PathMatchesMatcher.matches;
import static java.nio.file.AccessMode.EXECUTE;
import static java.nio.file.AccessMode.READ;
import static java.nio.file.AccessMode.WRITE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.ByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.nio.file.spi.FileSystemProvider;
import java.text.ParseException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class WindowsMemoryFileSystemTest {

  @RegisterExtension
  final WindowsFileSystemExtension extension = new WindowsFileSystemExtension();

  @Test
  void setAttributes() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    FileAttribute<?> hiddenAttribute = new StubFileAttribute<>("dos:hidden", true);

    Path hiddenPath = fileSystem.getPath("hidden");
    Files.createFile(hiddenPath, hiddenAttribute);
    DosFileAttributeView dosAttributeView = Files.getFileAttributeView(hiddenPath, DosFileAttributeView.class);
    assertTrue(dosAttributeView.readAttributes().isHidden());
  }

  @Test
  void pathToUri() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path path = fileSystem.getPath("C:\\file.txt");

    URI uri = path.toUri();
    assertEquals(uri, URI.create("memory:WindowsFileSystemRule:///C:/file.txt"));
    Path back = Paths.get(uri);
    assertEquals(path, back);
  }

  @Test
  void uriSingleSlash() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path path = fileSystem.getPath("C:\\file.txt");

    URI uri = URI.create("memory:WindowsFileSystemRule:/C:/file.txt");
    Path back = Paths.get(uri);
    assertEquals(path, back);
  }

  @Test
  void pathToWhiteSpace() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path path = fileSystem.getPath("C:\\z z");

    URI uri = path.toUri();
    Path back = Paths.get(uri);
    assertEquals(path, back);
  }

  @Test
  void dosAttributeNames() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path path = fileSystem.getPath("C:\\file.txt");

    Files.createFile(path);

    Map<String, Object> attributes = Files.readAttributes(path, "dos:*");
    List<String> expectedAttributeNames = Arrays.asList("readonly", "hidden", "system", "archive", // dos
            // basic
            "lastModifiedTime", "lastAccessTime", "creationTime", "size", "isRegularFile", "isDirectory", "isSymbolicLink", "isOther", "fileKey");
    assertEquals(new HashSet<>(expectedAttributeNames), attributes.keySet());
  }

  @Test
  void readAttributes() throws IOException, ParseException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path path = fileSystem.getPath("C:\\file.txt");

    Files.createFile(path);

    FileTime lastModifiedTime = FileTime.from(Instant.parse("2012-11-07T20:30:22.1111111Z"));
    FileTime lastAccessTime = FileTime.from(Instant.parse("2012-10-07T20:30:22.2222222Z"));
    FileTime createTime = FileTime.from(Instant.parse("2012-09-07T20:30:22.3333333Z"));

    BasicFileAttributeView basicFileAttributeView = Files.getFileAttributeView(path, BasicFileAttributeView.class);
    basicFileAttributeView.setTimes(lastModifiedTime, lastAccessTime, createTime);
    DosFileAttributeView dosFileAttributeView = Files.getFileAttributeView(path, DosFileAttributeView.class);
    dosFileAttributeView.setHidden(true);

    Map<String, Object> attributes = Files.readAttributes(path, "dos:lastModifiedTime,lastAccessTime,size,hidden");

    Map<String, Object> expected = new HashMap<>(4);
    expected.put("size", 0L);
    expected.put("lastModifiedTime", lastModifiedTime);
    expected.put("lastAccessTime", lastAccessTime);
    expected.put("hidden", true);

    assertEquals(expected, attributes);
  }

  @Test
  void windows() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path c1 = fileSystem.getPath("C:\\");
    Path c2 = fileSystem.getPath("c:\\");
    assertEquals("C:\\", c1.toString());
    assertEquals(c1.hashCode(), c2.hashCode());
    assertTrue(c1.startsWith(c2));
    assertTrue(c1.startsWith("c:\\"));
    assertEquals(c1, c2);

    c1 = fileSystem.getPath("C:\\TEMP");
    c2 = fileSystem.getPath("c:\\temp");
    assertEquals("C:\\TEMP", c1.toString());
    assertTrue(c1.startsWith(c2));
    assertTrue(c1.startsWith("c:\\"));
  }

  @Test
  void windowsDifferentFileSystems() throws IOException {
    URI uri1 = URI.create("memory:uri1");
    URI uri2 = URI.create("memory:uri2");
    Map<String, ?> env = MemoryFileSystemBuilder.newWindows().buildEnvironment();
    try (
            FileSystem fileSystem1 = FileSystems.newFileSystem(uri1, env);
            FileSystem fileSystem2 = FileSystems.newFileSystem(uri2, env)) {
      Path c1 = fileSystem1.getPath("C:\\");
      Path c2 = fileSystem2.getPath("C:\\\\");

      assertThat(c1, equalTo(c1));
      assertThat(c2, equalTo(c2));

      // different file systems
      assertThat(c1, not(equalTo(c2)));
      assertThat(c2, not(equalTo(c1)));
    }
  }



  @Test
  void forbiddenCharacters() {
    FileSystem fileSystem = this.extension.getFileSystem();
    for (char c : "\\/:?\"<>|".toCharArray()) {
      assertThrows(InvalidPathException.class,
              () -> fileSystem.getPath(Character.toString(c) + ".txt"),
              () -> c + " should be forbidden");
    }
  }

  @Test
  void windowsQuirky() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path c1 = fileSystem.getPath("C:\\");
    Path c2 = fileSystem.getPath("c:\\");
    assertEquals("c:\\", c2.toString());

    c1 = fileSystem.getPath("C:\\TEMP");
    c2 = fileSystem.getPath("c:\\temp");
    assertEquals("c:\\temp", c2.toString());
    assertEquals(c1.hashCode(), c2.hashCode());
  }

  @Test
  void checkAccess() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path file = fileSystem.getPath("file.txt");

    Files.createFile(file);
    DosFileAttributeView attributeView = Files.getFileAttributeView(file, DosFileAttributeView.class);
    DosFileAttributes attributes = attributeView.readAttributes();
    assertFalse(attributes.isReadOnly(), "is read only");

    FileSystemProvider provider = file.getFileSystem().provider();
    provider.checkAccess(file, READ);
    provider.checkAccess(file, WRITE);
    provider.checkAccess(file, EXECUTE);

    attributeView.setReadOnly(true);
    provider.checkAccess(file, READ);
    provider.checkAccess(file, EXECUTE);

    assertThrows(AccessDeniedException.class, () -> provider.checkAccess(file, WRITE), "write should not be permitted");
  }

  @Test
  void copyAttributes() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path source = fileSystem.getPath("source.txt");
    Path target = fileSystem.getPath("target.txt");

    Files.createFile(source);

    DosFileAttributeView sourceDosFileAttributeView = Files.getFileAttributeView(source, DosFileAttributeView.class);
    DosFileAttributes sourceDosAttributes = sourceDosFileAttributeView.readAttributes();
    assertFalse(sourceDosAttributes.isArchive());
    assertFalse(sourceDosAttributes.isHidden());
    assertFalse(sourceDosAttributes.isReadOnly());
    assertFalse(sourceDosAttributes.isSystem());

    sourceDosFileAttributeView.setArchive(true);
    sourceDosFileAttributeView.setHidden(true);
    sourceDosFileAttributeView.setReadOnly(true);
    sourceDosFileAttributeView.setSystem(true);

    Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);

    DosFileAttributeView targetDosFileAttributeView = Files.getFileAttributeView(target, DosFileAttributeView.class);
    DosFileAttributes targetDosAttributes = targetDosFileAttributeView.readAttributes();
    assertTrue(targetDosAttributes.isArchive());
    assertTrue(targetDosAttributes.isHidden());
    assertTrue(targetDosAttributes.isReadOnly());
    assertTrue(targetDosAttributes.isSystem());
  }

  @Test
  void dontCopyAttributes() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path source = fileSystem.getPath("source.txt");
    Path target = fileSystem.getPath("target.txt");

    Files.createFile(source);

    DosFileAttributeView sourceDosFileAttributeView = Files.getFileAttributeView(source, DosFileAttributeView.class);
    DosFileAttributes sourceDosAttributes = sourceDosFileAttributeView.readAttributes();
    assertFalse(sourceDosAttributes.isArchive());
    assertFalse(sourceDosAttributes.isHidden());
    assertFalse(sourceDosAttributes.isReadOnly());
    assertFalse(sourceDosAttributes.isSystem());

    sourceDosFileAttributeView.setArchive(true);
    sourceDosFileAttributeView.setHidden(true);
    sourceDosFileAttributeView.setReadOnly(true);
    sourceDosFileAttributeView.setSystem(true);

    Files.copy(source, target);

    DosFileAttributeView targetDosFileAttributeView = Files.getFileAttributeView(target, DosFileAttributeView.class);
    DosFileAttributes targetDosAttributes = targetDosFileAttributeView.readAttributes();
    assertFalse(targetDosAttributes.isArchive());
    assertFalse(targetDosAttributes.isHidden());
    assertFalse(targetDosAttributes.isReadOnly());
    assertFalse(targetDosAttributes.isSystem());
  }



  @Test
  void isHiddenPathResolution() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path hidden = fileSystem.getPath("hidden.txt");
    Files.createFile(hidden);

    DosFileAttributeView attributeView = Files.getFileAttributeView(hidden, DosFileAttributeView.class);
    attributeView.setHidden(true);

    assertThat(hidden, isHidden());

    hidden = fileSystem.getPath("hidden.txt/.././hidden.txt");
    assertThat(hidden, isHidden());
  }

  @Test
  void pathOrdering() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path lowerA = fileSystem.getPath("a");
    Path upperA = fileSystem.getPath("A");

    assertEquals(0, lowerA.compareTo(lowerA));
    assertEquals(0, upperA.compareTo(lowerA));
    assertEquals(0, lowerA.compareTo(upperA));
    assertEquals(0, upperA.compareTo(upperA));

    assertEquals(lowerA, lowerA);
    assertEquals(lowerA, upperA);
    assertEquals(upperA, lowerA);
    assertEquals(upperA, upperA);

    Path c = fileSystem.getPath("C:\\");
    Path d = fileSystem.getPath("D:\\");
    assertThat(c, lessThan(d));
    assertThat(d, greaterThan(c));
  }



  // https://bugs.openjdk.java.net/browse/JDK-8066915
  @Test
  void jdk8066915() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path directory = fileSystem.getPath("directory");
    Files.createDirectory(directory);

    try (ByteChannel channel = Files.newByteChannel(directory)) {
      fail("should not be able to create channel on directory");
    } catch (FileSystemException e) {
      // should reach here
      assertEquals(directory.toAbsolutePath().toString(), e.getFile(), "file");
    }

    try (ByteChannel channel = Files.newByteChannel(directory, StandardOpenOption.READ)) {
      fail("should not be able to create channel on directory");

    } catch (FileSystemException e) {
      // should reach here
      assertEquals(directory.toAbsolutePath().toString(), e.getFile(), "file");
    }

    try (ByteChannel channel = Files.newByteChannel(directory, StandardOpenOption.WRITE)) {
      fail("should not be able to create channel on directory");
    } catch (FileSystemException e) {
      // should reach here
      assertEquals(directory.toAbsolutePath().toString(), e.getFile(), "file");
    }
  }

  // https://bugs.openjdk.java.net/browse/JDK-8072495
  @Test
  void jdk8072495() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path x = fileSystem.getPath("x");
    Path empty = fileSystem.getPath("");
    Path actual = empty.relativize(x);
    assertEquals(x, actual);
  }

  @Test
  void preserveCase() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path originalPath = fileSystem.getPath("C:\\File0.txt");
    Files.createFile(originalPath);
    assertThat(fileSystem.getPath("C:\\file0.txt"), exists());
    boolean found = false;
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(originalPath.getParent())) {
      for (Path each : stream) {
        if ("File0.txt".equals(each.getFileName().toString())) {
          found = true;
          break;
        }
      }
    }
    assertTrue(found);
  }



  @Test
  void testPathMatherGlob() {
    FileSystem fileSystem = this.extension.getFileSystem();

    PathMatcher matcher = fileSystem.getPathMatcher("glob:*.{java,class}");
    assertThat(matcher, matches(fileSystem.getPath("Test.java")));
    assertThat(matcher, matches(fileSystem.getPath("Test.class")));
    assertThat(matcher, not(matches(fileSystem.getPath("Test.cpp"))));

    matcher = fileSystem.getPathMatcher("glob:*");
    assertThat(matcher, matches(fileSystem.getPath("Test.java")));
    assertThat(matcher, matches(fileSystem.getPath("Test.class")));
    assertThat(matcher, matches(fileSystem.getPath("Test.cpp")));
  }

  @Test
  void absoluteGlobPattern() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Files.createDirectories(fileSystem.getPath("C:\\folder\\child1"));
    Files.createDirectories(fileSystem.getPath("C:\\folder\\not-child"));

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(fileSystem.getPath("C:\\folder"), "C:\\folder\\child*")) {
      for (Path path : stream) {
        assertEquals("child1", path.getFileName().toString());
      }
    }
  }

  @Test
  @Disabled
  void relativePaths() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path relative = fileSystem.getPath("C:folder\\file.txt");
    Path absolute = fileSystem.getPath("C:\\folder\\file.txt");

    assertThat(relative, isRelative());
    assertThat(absolute, isAbsolute());
  }

  @Test
  void truncationViaSetTimes() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Instant mtime = Instant.parse("2019-02-27T12:37:03.123456789Z");
    Instant atime = Instant.parse("2019-02-27T12:37:03.223456789Z");
    Instant ctime = Instant.parse("2019-02-27T12:37:03.323456789Z");

    Path file = Files.createFile(fileSystem.getPath("C:\\file.txt"));
    BasicFileAttributeView view = Files.getFileAttributeView(file, BasicFileAttributeView.class);
    view.setTimes(FileTime.from(mtime), FileTime.from(atime), FileTime.from(ctime));

    BasicFileAttributes attributes = view.readAttributes();

    assertEquals(Instant.parse("2019-02-27T12:37:03.123456700Z"), attributes.lastModifiedTime().toInstant());
    assertEquals(Instant.parse("2019-02-27T12:37:03.223456700Z"), attributes.lastAccessTime().toInstant());
    assertEquals(Instant.parse("2019-02-27T12:37:03.323456700Z"), attributes.creationTime().toInstant());
  }

  @Test
  void truncationViaSet() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Instant atime = Instant.parse("2019-02-27T12:37:03.223456789Z");

    Path file = Files.createFile(fileSystem.getPath("C:\\file.txt"));
    Files.setAttribute(file, "lastAccessTime", FileTime.from(atime));

    BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
    assertEquals(Instant.parse("2019-02-27T12:37:03.223456700Z"), attributes.lastAccessTime().toInstant());
  }

  /**
   * Regression test for <a href="https://github.com/marschall/memoryfilesystem/issues/125">Issue 125</a>.
   *
   * @throws IOException if the test fails
   */
  @Test
  void mulitpleRoots() throws IOException {
    try (FileSystem fileSystem = MemoryFileSystemBuilder.newWindows().addRoot("D:\\").build()) {
      Files.createDirectories(fileSystem.getPath("C:\\folder\\child1"));
      Files.createDirectories(fileSystem.getPath("D:\\otherfolder\\child1"));
    }
  }

}

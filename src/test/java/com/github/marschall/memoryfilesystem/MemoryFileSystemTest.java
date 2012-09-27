package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.Constants.SAMPLE_ENV;
import static com.github.marschall.memoryfilesystem.Constants.SAMPLE_URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class MemoryFileSystemTest {

  @Rule
  public final FileSystemRule rule = new FileSystemRule();

  @Test(expected = IllegalArgumentException.class)
  public void emptySubPath() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    assertEquals(fileSystem.getPath(""), fileSystem.getPath("").subpath(0, 0));
  }

  @Test
  public void subPath() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    assertEquals(fileSystem.getPath("a"), fileSystem.getPath("a").subpath(0, 1));
    assertEquals(fileSystem.getPath("a"), fileSystem.getPath("/a").subpath(0, 1));
    assertEquals(fileSystem.getPath("b"), fileSystem.getPath("/a/b").subpath(1, 2));
    assertEquals(fileSystem.getPath("b"), fileSystem.getPath("a/b").subpath(1, 2));
    assertEquals(fileSystem.getPath("b"), fileSystem.getPath("/a/b/c").subpath(1, 2));
    assertEquals(fileSystem.getPath("b"), fileSystem.getPath("a/b/c").subpath(1, 2));

    assertEquals(fileSystem.getPath("a/b"), fileSystem.getPath("a/b").subpath(0, 2));
    assertEquals(fileSystem.getPath("a/b"), fileSystem.getPath("/a/b").subpath(0, 2));
    assertEquals(fileSystem.getPath("b/c"), fileSystem.getPath("/a/b/c").subpath(1, 3));
    assertEquals(fileSystem.getPath("b/c"), fileSystem.getPath("a/b/c").subpath(1, 3));
    assertEquals(fileSystem.getPath("b/c"), fileSystem.getPath("/a/b/c/d").subpath(1, 3));
    assertEquals(fileSystem.getPath("b/c"), fileSystem.getPath("a/b/c/c").subpath(1, 3));
  }

  @Test
  public void normalizeRoot() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path root = fileSystem.getPath("/");

    assertEquals(root, root.normalize());
  }

  @Test
  public void normalizeAbsolute() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();

    assertEquals(fileSystem.getPath("/a"), fileSystem.getPath("/a").normalize());
    assertEquals(fileSystem.getPath("/a"), fileSystem.getPath("/a/.").normalize());
    assertEquals(fileSystem.getPath("/a/b/c"), fileSystem.getPath("/a/b/./c").normalize());
    assertEquals(fileSystem.getPath("/a/b/c"), fileSystem.getPath("/a/b/c/.").normalize());
    assertEquals(fileSystem.getPath("/a/b/c"), fileSystem.getPath("/./a/b/c").normalize());
    assertEquals(fileSystem.getPath("/a/b/c"), fileSystem.getPath("/a/./b/c/.").normalize());
    assertEquals(fileSystem.getPath("/a"), fileSystem.getPath("/./a").normalize());
    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("/.").normalize());
    assertEquals(fileSystem.getPath("/a"), fileSystem.getPath("/a/.").normalize());
    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("/a/..").normalize());
    assertEquals(fileSystem.getPath("/a"), fileSystem.getPath("/a/b/..").normalize());
    assertEquals(fileSystem.getPath("/a/c"), fileSystem.getPath("/a/b/../c").normalize());
    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("/a/../..").normalize());
    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("/..").normalize());
    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("/../..").normalize());
    assertEquals(fileSystem.getPath("/a/b"), fileSystem.getPath("/../a/b").normalize());
    assertEquals(fileSystem.getPath("/a/b"), fileSystem.getPath("/../../a/b").normalize());
    assertEquals(fileSystem.getPath("/c"), fileSystem.getPath("/a/b/../../c").normalize());
  }

  @Test
  public void normalizeRelative() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();

    assertEquals(fileSystem.getPath("a"), fileSystem.getPath("a").normalize());
    assertEquals(fileSystem.getPath("a"), fileSystem.getPath("a/.").normalize());
    assertEquals(fileSystem.getPath(""), fileSystem.getPath("a/..").normalize());
    assertEquals(fileSystem.getPath(".."), fileSystem.getPath("..").normalize());
    assertEquals(fileSystem.getPath("../.."), fileSystem.getPath("../..").normalize());
    assertEquals(fileSystem.getPath("../.."), fileSystem.getPath(".././..").normalize());
    assertEquals(fileSystem.getPath("../../a/b/c"), fileSystem.getPath("../../a/b/c").normalize());
    assertEquals(fileSystem.getPath("../../a/b"), fileSystem.getPath("../../a/b/c/..").normalize());
    assertEquals(fileSystem.getPath("../../a/b"), fileSystem.getPath("../../a/b/c/./..").normalize());
  }

  @Test
  public void resolveAbsoluteOtherAbsolute() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path absolute = fileSystem.getPath("/a/b");

    assertEquals(absolute, fileSystem.getPath("").resolve(absolute));
    assertEquals(absolute, fileSystem.getPath("/").resolve(absolute));
    assertEquals(absolute, fileSystem.getPath("c/d").resolve(absolute));
    assertEquals(absolute, fileSystem.getPath("/c/d").resolve(absolute));
  }

  @Test
  public void resolveAbsoluteOtherRelative() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path realtive = fileSystem.getPath("a/b");

    assertEquals(fileSystem.getPath("a/b"), fileSystem.getPath("").resolve(realtive));
    assertEquals(fileSystem.getPath("/a/b"), fileSystem.getPath("/").resolve(realtive));
    assertEquals(fileSystem.getPath("c/d/a/b"), fileSystem.getPath("c/d").resolve(realtive));
    assertEquals(fileSystem.getPath("/c/d/a/b"), fileSystem.getPath("/c/d").resolve(realtive));
  }


  @Test
  public void resolveAbsoluteOtherAbsoluteString() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    String absolute = "/a/b";
    Path absolutePath = fileSystem.getPath("/a/b");

    assertEquals(absolutePath, fileSystem.getPath("").resolve(absolute));
    assertEquals(absolutePath, fileSystem.getPath("/").resolve(absolute));
    assertEquals(absolutePath, fileSystem.getPath("c/d").resolve(absolute));
    assertEquals(absolutePath, fileSystem.getPath("/c/d").resolve(absolute));
  }

  @Test
  public void resolveAbsoluteOtherRelativeString() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    String realtive = "a/b";

    assertEquals(fileSystem.getPath("a/b"), fileSystem.getPath("").resolve(realtive));
    assertEquals(fileSystem.getPath("/a/b"), fileSystem.getPath("/").resolve(realtive));
    assertEquals(fileSystem.getPath("c/d/a/b"), fileSystem.getPath("c/d").resolve(realtive));
    assertEquals(fileSystem.getPath("/c/d/a/b"), fileSystem.getPath("/c/d").resolve(realtive));
  }


  @Test
  public void resolveSiblingAgainstRoot() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path root = fileSystem.getPath("/");

    assertEquals(fileSystem.getPath("a"), root.resolveSibling(fileSystem.getPath("a")));
    assertEquals(fileSystem.getPath("a/b"), root.resolveSibling(fileSystem.getPath("a/b")));
    assertEquals(fileSystem.getPath("/a"), root.resolveSibling(fileSystem.getPath("/a")));
    assertEquals(fileSystem.getPath("/a/b"), root.resolveSibling(fileSystem.getPath("/a/b")));
    assertEquals(fileSystem.getPath(""), root.resolveSibling(fileSystem.getPath("")));
  }

  @Test
  public void resolveSiblingAgainstRootString() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path root = fileSystem.getPath("/");

    assertEquals(fileSystem.getPath("a"), root.resolveSibling("a"));
    assertEquals(fileSystem.getPath("a/b"), root.resolveSibling("a/b"));
    assertEquals(fileSystem.getPath("/a"), root.resolveSibling("/a"));
    assertEquals(fileSystem.getPath("/a/b"), root.resolveSibling("/a/b"));
    assertEquals(fileSystem.getPath(""), root.resolveSibling(""));
  }


  @Test
  public void relativizeAbsolute() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path first = fileSystem.getPath("/a/b");
    Path second = fileSystem.getPath("/a/b/c");

    assertEquals(fileSystem.getPath("c"), first.relativize(second));
    assertEquals(fileSystem.getPath(".."), second.relativize(first));
    assertEquals(fileSystem.getPath(""), first.relativize(first));

    // ---

    first = fileSystem.getPath("/a/b");
    second = fileSystem.getPath("/a/b/c/d");

    assertEquals(fileSystem.getPath("c/d"), first.relativize(second));
    assertEquals(fileSystem.getPath("../.."), second.relativize(first));
    assertEquals(fileSystem.getPath(""), first.relativize(first));

    // ---

    first = fileSystem.getPath("/a/b");
    second = fileSystem.getPath("/c");

    assertEquals(fileSystem.getPath("../../c"), first.relativize(second));
    assertEquals(fileSystem.getPath("../a/b"), second.relativize(first));
    assertEquals(fileSystem.getPath(""), first.relativize(first));

    // ---

    first = fileSystem.getPath("/a/b");
    second = fileSystem.getPath("/c/d");

    assertEquals(fileSystem.getPath("../../c/d"), first.relativize(second));
    assertEquals(fileSystem.getPath("../../a/b"), second.relativize(first));
    assertEquals(fileSystem.getPath(""), first.relativize(first));
  }

  @Test(expected = IllegalArgumentException.class)
  public void relativizeAbsoluteUnsupported1() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path first = fileSystem.getPath("/a/b");
    Path second = fileSystem.getPath("c");
    first.relativize(second);
  }

  @Test(expected = IllegalArgumentException.class)
  public void relativizeAbsoluteUnsupported2() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path first = fileSystem.getPath("/a/b");
    Path second = fileSystem.getPath("c");
    second.relativize(first);
  }


  @Test
  public void relativizeRelative() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path first = fileSystem.getPath("a/b");
    Path second = fileSystem.getPath("a/b/c");

    assertEquals(fileSystem.getPath("c"), first.relativize(second));
    assertEquals(fileSystem.getPath(".."), second.relativize(first));
    assertEquals(fileSystem.getPath(""), first.relativize(first));

    // ---

    first = fileSystem.getPath("a/b");
    second = fileSystem.getPath("a/b/c/d");

    assertEquals(fileSystem.getPath("c/d"), first.relativize(second));
    assertEquals(fileSystem.getPath("../.."), second.relativize(first));
    assertEquals(fileSystem.getPath(""), first.relativize(first));

    // ---

    first = fileSystem.getPath("a/b");
    second = fileSystem.getPath("c");

    assertEquals(fileSystem.getPath("../../c"), first.relativize(second));
    assertEquals(fileSystem.getPath("../a/b"), second.relativize(first));
    assertEquals(fileSystem.getPath(""), first.relativize(first));

    // ---

    first = fileSystem.getPath("a/b");
    second = fileSystem.getPath("c/d");

    assertEquals(fileSystem.getPath("../../c/d"), first.relativize(second));
    assertEquals(fileSystem.getPath("../../a/b"), second.relativize(first));
    assertEquals(fileSystem.getPath(""), first.relativize(first));
  }


  @Test
  public void relativizeRelativeRoot() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path first = fileSystem.getPath("/");
    Path second = fileSystem.getPath("/a/b");

    assertEquals(fileSystem.getPath("a/b"), first.relativize(second));
    assertEquals(fileSystem.getPath("../.."), second.relativize(first));
    assertEquals(fileSystem.getPath(""), first.relativize(first));
  }

  @Test
  public void absoluteIterator() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path usrBin = fileSystem.getPath("/usr/bin");
    Iterable<String> expected = Arrays.asList("usr", "bin");
    assertIterator(fileSystem, usrBin, expected);
  }

  @Test
  public void relativeIterator() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path usrBin = fileSystem.getPath("usr/bin");
    Iterable<String> expected = Arrays.asList("usr", "bin");
    assertIterator(fileSystem, usrBin, expected);
  }

  private void assertIterator(FileSystem fileSystem, Path path, Iterable<String> expected) {
    Iterator<Path> actualIterator = path.iterator();
    Iterator<String> expectedIterator = expected.iterator();
    while (actualIterator.hasNext()) {
      Path actualPath = actualIterator.next();
      try {
        actualIterator.remove();
        fail("path iterator should not support #remove()");
      } catch (UnsupportedOperationException e) {
        assertTrue("path iterator #remove() should throw UnsupportedOperationException", true);
      }

      assertTrue(expectedIterator.hasNext());
      String expectedName = (String) expectedIterator.next();
      Path expectedPath = fileSystem.getPath(expectedName);

      assertEquals(expectedPath, actualPath);
      assertFalse(actualPath.isAbsolute());
    }
    assertFalse(expectedIterator.hasNext());
  }

  @Test
  public void getFileName() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path usrBin = fileSystem.getPath("/usr/bin");
    Path bin = fileSystem.getPath("bin");

    Path fileName = usrBin.getFileName();
    assertNotNull(fileName);

    assertEquals(fileName, bin);
    assertFalse(fileName.isAbsolute());
  }

  @Test
  public void absoluteGetParent() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path usrBin = fileSystem.getPath("/usr/bin");
    Path usr = fileSystem.getPath("/usr");

    assertEquals(usr, usrBin.getParent());
    assertTrue(usrBin.getParent().isAbsolute());
    Path root = fileSystem.getRootDirectories().iterator().next();
    assertEquals(root, usr.getParent());
    assertTrue(usr.getParent().isAbsolute());
  }

  @Test
  public void relativeGetParent() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path usrBin = fileSystem.getPath("usr/bin");
    Path usr = fileSystem.getPath("usr");

    assertEquals(usr, usrBin.getParent());
    assertFalse(usrBin.getParent().isAbsolute());
    assertNull(usr.getParent());
  }

  @Test(expected = IllegalArgumentException.class)
  public void absoluteGetName0() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path usrBin = fileSystem.getPath("/usr/bin");
    usrBin.getName(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void absoluteGetNameToLong() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path usrBin = fileSystem.getPath("/usr/bin");
    usrBin.getName(2);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyGetName() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path empty = fileSystem.getPath("");
    empty.getName(0);
  }

  @Test
  public void absoluteGetName() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path usrBin = fileSystem.getPath("/usr/bin");
    Path usr = fileSystem.getPath("usr");
    assertEquals(usr, usrBin.getName(0));
    Path bin = fileSystem.getPath("bin");
    assertEquals(bin, usrBin.getName(1));
  }

  @Test
  public void relativeGetName() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path usrBin = fileSystem.getPath("usr/bin");
    Path usr = fileSystem.getPath("usr");
    assertEquals(usr, usrBin.getName(0));
    Path bin = fileSystem.getPath("bin");
    assertEquals(bin, usrBin.getName(1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void relativeGetName0() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path usrBin = fileSystem.getPath("usr/bin");
    usrBin.getName(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void relativeGetNameToLong() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path usrBin = fileSystem.getPath("usr/bin");
    usrBin.getName(2);
  }

  @Test
  public void emptyPath() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path path = fileSystem.getPath("");
    assertFalse(path.isAbsolute());
    assertNull(path.getRoot());
  }

  @Test
  public void getNameCount() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path usrBin = fileSystem.getPath("/usr/bin");
    assertEquals(2, usrBin.getNameCount());

    usrBin = fileSystem.getPath("usr/bin");
    assertEquals(2, usrBin.getNameCount());
  }

  @Test
  public void isReadOnly() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    assertFalse(fileSystem.isReadOnly());
  }

  @Test
  public void absolutePaths() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path path = fileSystem.getPath("/");
    assertTrue(path.isAbsolute());
    assertSame(path, path.toAbsolutePath());

    path = fileSystem.getPath("/", "sample");
    assertTrue(path.isAbsolute());
    assertSame(path, path.toAbsolutePath());
    assertNotNull(path.getRoot());
    assertSame(getRoot(fileSystem), path.getRoot());
  }

  @Test
  public void relativePaths() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path path = fileSystem.getPath("sample");
    assertFalse(path.isAbsolute());
    assertNull(path.getRoot());
  }

  private Path getRoot(FileSystem fileSystem) {
    Iterable<Path> rootDirectories = fileSystem.getRootDirectories();
    Iterator<Path> iterator = rootDirectories.iterator();
    Path root = iterator.next();
    assertFalse(iterator.hasNext());
    return root;
  }

  @Test
  public void supportedFileAttributeViews() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    assertEquals(Collections.singleton("basic"), fileSystem.supportedFileAttributeViews());
  }

  @Test
  public void pathToString() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path path = fileSystem.getPath("/");
    assertEquals("/", path.toString());

    path = fileSystem.getPath("/home");
    assertEquals("/home", path.toString());

    path = fileSystem.getPath("/home/pmarscha");
    assertEquals("/home/pmarscha", path.toString());

    path = fileSystem.getPath("home");
    assertEquals("home", path.toString());

    path = fileSystem.getPath("home/pmarscha");
    assertEquals("home/pmarscha", path.toString());

    path = fileSystem.getPath("home/./../pmarscha");
    assertEquals("home/./../pmarscha", path.toString());
  }

  @Test
  public void defaultSeparator() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    assertEquals("/", fileSystem.getSeparator());
  }


  @Test(expected = IllegalArgumentException.class)
  public void slash() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path path = fileSystem.getPath("/");
    path.subpath(0, 1);
  }

  @Test(expected = IOException.class)
  public void createDirectoryNoParent() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path homePmarscha = fileSystem.getPath("/home/pmarscha");
    assertFalse(Files.exists(homePmarscha));
    Files.createDirectory(homePmarscha);
    assertTrue(Files.exists(homePmarscha));
  }


  @Test
  public void createDirectories() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path homePmarscha = fileSystem.getPath("/home/pmarscha");
    assertFalse(Files.exists(homePmarscha));
    Files.createDirectories(homePmarscha);
    assertTrue(Files.exists(homePmarscha));
  }


  @Test
  public void createDirectory() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path home = fileSystem.getPath("/home");
    assertFalse(Files.exists(home));
    Files.createDirectory(home);
    assertTrue(Files.exists(home));
    assertTrue(Files.isDirectory(home));
    assertFalse(Files.isRegularFile(home));
  }

  @Test(expected = FileAlreadyExistsException.class)
  public void createDirectoryAlreadyExists() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path home = fileSystem.getPath("/home");
    assertFalse(Files.exists(home));
    Files.createDirectory(home);
    assertTrue(Files.exists(home));
    Files.createDirectory(home);
  }

  @Test
  public void getRootDirectories() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Iterator<Path> directories = fileSystem.getRootDirectories().iterator();
    assertTrue(directories.hasNext());
    directories.next();
    try {
      directories.remove();
      fail("root directories iterator should not support remove");
    } catch (UnsupportedOperationException e) {
      // should reach here
    }
    assertFalse(directories.hasNext());
  }

}

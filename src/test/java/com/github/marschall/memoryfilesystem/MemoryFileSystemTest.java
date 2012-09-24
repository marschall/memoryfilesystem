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
import org.junit.Test;

import com.github.marschall.memoryfilesystem.EnvironmentBuilder;
import com.github.marschall.memoryfilesystem.MemoryFileSystem;
import com.github.marschall.memoryfilesystem.MemoryFileSystemProperties;

public class MemoryFileSystemTest {


  @Test
  public void close() throws IOException {
    FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV);

    // file system should be open
    assertNotNull(fileSystem);
    assertTrue(fileSystem instanceof MemoryFileSystem);
    assertTrue(fileSystem.isOpen());

    // creating a new one should fail
    try {
      FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV);
      fail("file system " + SAMPLE_URI + " already exists");
    } catch (FileSystemAlreadyExistsException e) {
      //should reach here
    }

    // closing should work
    fileSystem.close();
    assertFalse(fileSystem.isOpen());

    // closing a second time should work
    fileSystem.close();
    assertFalse(fileSystem.isOpen());

    // after closing we should be able to create a new one again
    try (FileSystem secondFileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      assertNotNull(secondFileSystem);
    }
  }
  
  
  @Test
  public void relativizeAbsolute() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
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
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void relativizeAbsoluteUnsupported1() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      Path first = fileSystem.getPath("/a/b");
      Path second = fileSystem.getPath("c");
      first.relativize(second);
    }
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void relativizeAbsoluteUnsupported2() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      Path first = fileSystem.getPath("/a/b");
      Path second = fileSystem.getPath("c");
      second.relativize(first);
    }
  }
  
  
  @Test
  public void relativizeRelative() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
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
  }
  

  @Test
  public void relativizeRelativeRoot() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      Path first = fileSystem.getPath("/");
      Path second = fileSystem.getPath("/a/b");
      
      assertEquals(fileSystem.getPath("a/b"), first.relativize(second));
      assertEquals(fileSystem.getPath("../.."), second.relativize(first));
      assertEquals(fileSystem.getPath(""), first.relativize(first));
    }
  }
  
  @Test
  public void absoluteIterator() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      Path usrBin = fileSystem.getPath("/usr/bin");
      Iterable<String> expected = Arrays.asList("usr", "bin");
      assertIterator(fileSystem, usrBin, expected);
    }
  }
  
  @Test
  public void relativeIterator() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      Path usrBin = fileSystem.getPath("usr/bin");
      Iterable<String> expected = Arrays.asList("usr", "bin");
      assertIterator(fileSystem, usrBin, expected);
    }
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
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      Path usrBin = fileSystem.getPath("/usr/bin");
      Path bin = fileSystem.getPath("bin");
      
      Path fileName = usrBin.getFileName();
      assertNotNull(fileName);
      
      assertEquals(fileName, bin);
      assertFalse(fileName.isAbsolute());
    }
  }
  
  @Test
  public void absoluteGetParent() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      Path usrBin = fileSystem.getPath("/usr/bin");
      Path usr = fileSystem.getPath("/usr");
      
      assertEquals(usr, usrBin.getParent());
      assertTrue(usrBin.getParent().isAbsolute());
      Path root = fileSystem.getRootDirectories().iterator().next();
      assertEquals(root, usr.getParent());
      assertTrue(usr.getParent().isAbsolute());
    }
  }
  
  @Test
  public void relativeGetParent() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      Path usrBin = fileSystem.getPath("usr/bin");
      Path usr = fileSystem.getPath("usr");
      
      assertEquals(usr, usrBin.getParent());
      assertFalse(usrBin.getParent().isAbsolute());
      assertNull(usr.getParent());
    }
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void absoluteGetName0() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      Path usrBin = fileSystem.getPath("/usr/bin");
      usrBin.getName(-1);
    }
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void absoluteGetNameToLong() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      Path usrBin = fileSystem.getPath("/usr/bin");
      usrBin.getName(2);
    }
  }
  
  @Test
  public void absoluteGetName() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      Path usrBin = fileSystem.getPath("/usr/bin");
      Path usr = fileSystem.getPath("/usr");
      assertEquals(usr, usrBin.getName(0));
    }
  }
  
  @Test
  public void relativeGetName() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      Path usrBin = fileSystem.getPath("usr/bin");
      Path usr = fileSystem.getPath("usr");
      assertEquals(usr, usrBin.getName(0));
    }
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void relativeGetName0() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      Path usrBin = fileSystem.getPath("usr/bin");
      usrBin.getName(-1);
    }
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void relativeGetNameToLong() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      Path usrBin = fileSystem.getPath("usr/bin");
      usrBin.getName(2);
    }
  }
  
  @Test
  public void emptyPath() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      Path path = fileSystem.getPath("");
      assertFalse(path.isAbsolute());
      assertNull(path.getRoot());
    }
  }
  
  @Test
  public void getNameCount() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      Path usrBin = fileSystem.getPath("/usr/bin");
      assertEquals(2, usrBin.getNameCount());
      
      usrBin = fileSystem.getPath("usr/bin");
      assertEquals(2, usrBin.getNameCount());
    }
  }

  @Test
  public void isReadOnly() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      assertFalse(fileSystem.isReadOnly());
    }
  }
  
  @Test
  public void absolutePaths() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      Path path = fileSystem.getPath("/");
      assertTrue(path.isAbsolute());
      assertSame(path, path.toAbsolutePath());
      
      path = fileSystem.getPath("/", "sample");
      assertTrue(path.isAbsolute());
      assertSame(path, path.toAbsolutePath());
      assertNotNull(path.getRoot());
      assertSame(getRoot(fileSystem), path.getRoot());
    }
  }
  
  @Test
  public void relativePaths() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      Path path = fileSystem.getPath("sample");
      assertFalse(path.isAbsolute());
      assertNull(path.getRoot());
    }
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
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      assertEquals(Collections.singleton("basic"), fileSystem.supportedFileAttributeViews());
    }
  }

  @Test
  public void lookupPrincipalByName() throws IOException {
    FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV);
    try {
      UserPrincipalLookupService userPrincipalLookupService = fileSystem.getUserPrincipalLookupService();
      String userName = System.getProperty("user.name");
      UserPrincipal user = userPrincipalLookupService.lookupPrincipalByName(userName);
      assertEquals(userName, user.getName());

      fileSystem.close();
      try {
        userPrincipalLookupService.lookupPrincipalByName(userName);
        fail("UserPrincipalLookupService should be invalid when file system is closed");
      } catch (ClosedFileSystemException e) {
        // should reach here
      }
    } finally {
      fileSystem.close();
    }
  }
  
  @Test
  public void pathToString() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
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
  }

  @Test
  public void defaultSeparator() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      assertEquals("/", fileSystem.getSeparator());
    }
  }
  
  @Test
  @Ignore("FIXME") //FIXME
  public void windows() throws IOException {
    URI uri = URI.create("memory:uri");
    Map<String, ?> env = EnvironmentBuilder.newWindows().build();
    try (FileSystem fileSystem = FileSystems.newFileSystem(uri, env)) {
      Path c1 = fileSystem.getPath("C:\\");
      Path c2 = fileSystem.getPath("c:\\");
      assertEquals(c1, c2);
      assertEquals("C:\\", c1.toString());
      assertEquals("c:\\", c2.toString());
      assertTrue(c1.startsWith(c2));
      assertTrue(c1.startsWith("c:\\"));
    }
  }

  @Test
  public void customSeparator() throws IOException {
    Map<String, Object> env = Collections.singletonMap(MemoryFileSystemProperties.DEFAULT_NAME_SEPARATOR_PROPERTY, (Object) "\\");
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, env)) {
      assertEquals("\\", fileSystem.getSeparator());
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidCustomSeparator() throws IOException {
    Map<String, Object> env = Collections.singletonMap(MemoryFileSystemProperties.DEFAULT_NAME_SEPARATOR_PROPERTY, (Object) "\u2603");
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, env)) {
      fail("unicode snow man should not be allowed as separator");
    }
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void slash() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      Path path = fileSystem.getPath("/");
      path.subpath(0, 1);
    }
  }
  
  @Test(expected = IOException.class)
  public void createDirectoryNoParent() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      Path homePmarscha = fileSystem.getPath("/home/pmarscha");
      assertFalse(Files.exists(homePmarscha));
      Files.createDirectory(homePmarscha);
      assertTrue(Files.exists(homePmarscha));
    }
  }
  

  @Test
  public void createDirectories() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      Path homePmarscha = fileSystem.getPath("/home/pmarscha");
      assertFalse(Files.exists(homePmarscha));
      Files.createDirectories(homePmarscha);
      assertTrue(Files.exists(homePmarscha));
    }
  }

  
  @Test
  public void createDirectory() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      Path home = fileSystem.getPath("/home");
      assertFalse(Files.exists(home));
      Files.createDirectory(home);
      assertTrue(Files.exists(home));
      assertTrue(Files.isDirectory(home));
      assertFalse(Files.isRegularFile(home));
    }
  }

  @Test(expected = FileAlreadyExistsException.class)
  public void createDirectoryAlreadyExists() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      Path home = fileSystem.getPath("/home");
      assertFalse(Files.exists(home));
      Files.createDirectory(home);
      assertTrue(Files.exists(home));
      Files.createDirectory(home);
    }
  }

  @Test
  public void getRootDirectories() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
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

}

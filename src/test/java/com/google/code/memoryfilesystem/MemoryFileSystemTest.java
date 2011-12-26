package com.google.code.memoryfilesystem;

import java.io.IOException;
import java.net.URI;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.junit.Test;

import static com.google.code.memoryfilesystem.Constants.SAMPLE_ENV;
import static com.google.code.memoryfilesystem.Constants.SAMPLE_URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
  public void defaultSeparator() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      assertEquals("/", fileSystem.getSeparator());
    }
  }
  
  @Test
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

package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.Constants.SAMPLE_ENV;
import static com.github.marschall.memoryfilesystem.Constants.SAMPLE_URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

public class CustomMemoryFileSystemTest {

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

}

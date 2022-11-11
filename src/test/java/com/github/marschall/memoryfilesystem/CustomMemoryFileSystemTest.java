package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.Constants.SAMPLE_ENV;
import static com.github.marschall.memoryfilesystem.Constants.SAMPLE_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CustomMemoryFileSystemTest {


  @Test
  void getFileSystemUriClosed() throws IOException {
    URI uri = URI.create("memory:getFileSystemUriClosed");
    Map<String, ?> env = Collections.<String, Object>emptyMap();
    try (FileSystem fileSystem = FileSystems.newFileSystem(uri, env)) {
      assertSame(fileSystem, FileSystems.getFileSystem(uri));
    }
    // file system is closed now
    assertThrows(FileSystemNotFoundException.class,
            () -> FileSystems.getFileSystem(uri),
            "file system should not exist anymore");
  }

  @Test
  void lookupPrincipalByName() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      UserPrincipalLookupService userPrincipalLookupService = fileSystem.getUserPrincipalLookupService();
      String userName = System.getProperty("user.name");
      UserPrincipal user = userPrincipalLookupService.lookupPrincipalByName(userName);
      assertEquals(userName, user.getName());

      fileSystem.close();
      assertThrows(ClosedFileSystemException.class,
              () -> userPrincipalLookupService.lookupPrincipalByName(userName),
              "UserPrincipalLookupService should be invalid when file system is closed");
    }
  }

  @Test
  void regressionIssue46() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      Path path = fileSystem.getPath("existing.zip");
      Files.createFile(path);
      FileTime time = FileTime.fromMillis(System.currentTimeMillis());
      Files.setAttribute(path, "basic:lastModifiedTime", time);
    }
  }

  @Test
  void close() throws IOException {
    FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV);

    // file system should be open
    assertNotNull(fileSystem);
    assertTrue(fileSystem instanceof MemoryFileSystem);
    assertTrue(fileSystem.isOpen());

    // creating a new one should fail
    assertThrows(FileSystemAlreadyExistsException.class,
            () -> FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV),
            () -> "file system " + SAMPLE_URI + " already exists");

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
  void customSeparator() throws IOException {
    Map<String, Object> env = Collections.singletonMap(MemoryFileSystemProperties.DEFAULT_NAME_SEPARATOR_PROPERTY, (Object) "\\");
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, env)) {
      assertEquals("\\", fileSystem.getSeparator());
    }
  }

  @Test
  void invalidCustomSeparator() {
    Map<String, Object> env = Collections.singletonMap(MemoryFileSystemProperties.DEFAULT_NAME_SEPARATOR_PROPERTY, (Object) "\u2603");
    assertThrows(IllegalArgumentException.class, () -> FileSystems.newFileSystem(SAMPLE_URI, env), "unicode snow man should not be allowed as separator");
  }

}

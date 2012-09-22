package com.google.code.memoryfilesystem;

import static com.google.code.memoryfilesystem.Constants.SAMPLE_ENV;
import static com.google.code.memoryfilesystem.Constants.SAMPLE_URI;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;

import org.junit.Ignore;
import org.junit.Test;

public class MemoryFileSystemUninstallerTest {
  
  @Test
  public void isInstalled() throws Exception {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      //do nothing
    }
    assertTrue(MemoryFileSystemUninstaller.isInstalled());
  }
  
  @Test
  @Ignore("breaks everything")
  public void uninstall() throws Exception {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      //do nothing
    }
    assertTrue(MemoryFileSystemUninstaller.isInstalled());
    
    MemoryFileSystemUninstaller.uninstall();
    assertFalse(MemoryFileSystemUninstaller.isInstalled());
  }

}

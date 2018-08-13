package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.Constants.SAMPLE_ENV;
import static com.github.marschall.memoryfilesystem.Constants.SAMPLE_URI;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.spi.FileSystemProvider;

import org.junit.Test;

public class MemoryFileSystemProviderTest {


  @Test
  public void checkInstallation() {
    boolean found = false;
    for (FileSystemProvider provider : FileSystemProvider.installedProviders()) {
      if (provider.getScheme().equals(MemoryFileSystemProvider.SCHEME)) {
        found = true;
      }
    }
    assertTrue(found);
  }

  @Test(expected = FileSystemNotFoundException.class)
  public void getNotExistingFileSystem() {
    FileSystems.getFileSystem(SAMPLE_URI);
  }

  @Test
  public void getFileSystem() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
      assertSame(fileSystem, FileSystems.getFileSystem(SAMPLE_URI));
    }

  }


}

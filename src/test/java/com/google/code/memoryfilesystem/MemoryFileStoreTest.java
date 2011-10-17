package com.google.code.memoryfilesystem;

import static com.google.code.memoryfilesystem.Constants.SAMPLE_URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.Iterator;

import org.junit.Rule;
import org.junit.Test;

public class MemoryFileStoreTest {

  @Rule
  public final FileStoreRule rule = new FileStoreRule();

  @Test
  public void onlyOneFileStore() throws IOException {
    try (FileSystem fileSystem = FileSystems.getFileSystem(SAMPLE_URI)) {
      Iterator<FileStore> fileStores = fileSystem.getFileStores().iterator();
      assertTrue(fileStores.hasNext());
      fileStores.next();
      try {
        fileStores.remove();
        fail("file store iterator should not support remove");
      } catch (UnsupportedOperationException e) {
        // should reach here
      }
      assertFalse(fileStores.hasNext());
    }
  }

  @Test
  public void isReadOnly() {
    assertFalse(rule.getFileStore().isReadOnly());
  }

  @Test
  public void name() {
    assertEquals("name", rule.getFileStore().name());
  }

  @Test
  public void type() {
    assertEquals(MemoryFileSystemProvider.SCHEME, rule.getFileStore().type());
  }

}

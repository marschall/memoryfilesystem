package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.Constants.SAMPLE_URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.Iterator;

import org.junit.Rule;
import org.junit.Test;

import com.github.marschall.memoryfilesystem.MemoryFileSystemProvider;

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
  
  @Test
  public void supportsFileAttributeViewClass() {
    assertTrue(rule.getFileStore().supportsFileAttributeView(BasicFileAttributeView.class));
    assertFalse(rule.getFileStore().supportsFileAttributeView(PosixFileAttributeView.class));
    assertFalse(rule.getFileStore().supportsFileAttributeView(DosFileAttributeView.class));
    assertFalse(rule.getFileStore().supportsFileAttributeView(FileOwnerAttributeView.class));
    assertFalse(rule.getFileStore().supportsFileAttributeView(UserDefinedFileAttributeView.class));
    assertFalse(rule.getFileStore().supportsFileAttributeView(AclFileAttributeView.class));
    
  }
  
  @Test
  public void supportsFileAttributeView() {
    assertTrue(rule.getFileStore().supportsFileAttributeView("basic"));
    assertFalse(rule.getFileStore().supportsFileAttributeView("posix"));
    assertFalse(rule.getFileStore().supportsFileAttributeView("dos"));
    assertFalse(rule.getFileStore().supportsFileAttributeView("owner"));
    assertFalse(rule.getFileStore().supportsFileAttributeView("user"));
    assertFalse(rule.getFileStore().supportsFileAttributeView("acl"));
  }

}

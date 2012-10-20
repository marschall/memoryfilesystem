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
    assertFalse(this.rule.getFileStore().isReadOnly());
  }

  @Test
  public void name() {
    assertEquals("name", this.rule.getFileStore().name());
  }

  @Test
  public void type() {
    assertEquals(MemoryFileSystemProvider.SCHEME, this.rule.getFileStore().type());
  }

  @Test
  public void supportsFileAttributeViewClass() {
    assertTrue(this.rule.getFileStore().supportsFileAttributeView(BasicFileAttributeView.class));
    assertFalse(this.rule.getFileStore().supportsFileAttributeView(PosixFileAttributeView.class));
    assertFalse(this.rule.getFileStore().supportsFileAttributeView(DosFileAttributeView.class));
    assertFalse(this.rule.getFileStore().supportsFileAttributeView(FileOwnerAttributeView.class));
    assertFalse(this.rule.getFileStore().supportsFileAttributeView(UserDefinedFileAttributeView.class));
    assertFalse(this.rule.getFileStore().supportsFileAttributeView(AclFileAttributeView.class));

  }

  @Test
  public void supportsFileAttributeView() {
    assertTrue(this.rule.getFileStore().supportsFileAttributeView(FileAttributeViews.BASIC));
    assertFalse(this.rule.getFileStore().supportsFileAttributeView(FileAttributeViews.POSIX));
    assertFalse(this.rule.getFileStore().supportsFileAttributeView(FileAttributeViews.DOS));
    assertFalse(this.rule.getFileStore().supportsFileAttributeView(FileAttributeViews.OWNER));
    assertFalse(this.rule.getFileStore().supportsFileAttributeView(FileAttributeViews.USER));
    assertFalse(this.rule.getFileStore().supportsFileAttributeView(FileAttributeViews.ACL));
  }

}

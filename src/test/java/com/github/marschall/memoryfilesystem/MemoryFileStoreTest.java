package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.Constants.SAMPLE_URI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class MemoryFileStoreTest {

  @RegisterExtension
  final FileStoreExtension extension = new FileStoreExtension();

  @Test
  void onlyOneFileStore() throws IOException {
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
  void isReadOnly() {
    assertFalse(this.extension.getFileStore().isReadOnly());
  }

  @Test
  void name() {
    assertEquals("name", this.extension.getFileStore().name());
  }

  @Test
  void type() {
    assertEquals(MemoryFileSystemProvider.SCHEME, this.extension.getFileStore().type());
  }

  @Test
  void supportsFileAttributeViewClass() {
    FileStore fileStore = this.extension.getFileStore();
    assertTrue(fileStore.supportsFileAttributeView(BasicFileAttributeView.class));
    assertFalse(fileStore.supportsFileAttributeView(PosixFileAttributeView.class));
    assertFalse(fileStore.supportsFileAttributeView(DosFileAttributeView.class));
    assertFalse(fileStore.supportsFileAttributeView(FileOwnerAttributeView.class));
    assertFalse(fileStore.supportsFileAttributeView(UserDefinedFileAttributeView.class));
    assertFalse(fileStore.supportsFileAttributeView(AclFileAttributeView.class));
  }

  @Test
  void fileSizes() throws IOException {
    FileStore fileStore = this.extension.getFileStore();
    long totalSpace = fileStore.getTotalSpace();
    long unallocatedSpace = fileStore.getUnallocatedSpace();
    long usableSpace = fileStore.getUsableSpace();

    assertThat("total space", totalSpace, greaterThan(0L));
    assertThat("unallocated space", unallocatedSpace, greaterThan(0L));
    assertThat("usable space", usableSpace, greaterThan(0L));

    assertThat("usable space", usableSpace, lessThan(totalSpace));
    assertThat("unallocated space", unallocatedSpace, lessThan(totalSpace));
  }

  @Test
  void supportsFileAttributeView() {
    FileStore fileStore = this.extension.getFileStore();
    assertTrue(fileStore.supportsFileAttributeView(FileAttributeViews.BASIC));
    assertFalse(fileStore.supportsFileAttributeView(FileAttributeViews.POSIX));
    assertFalse(fileStore.supportsFileAttributeView(FileAttributeViews.DOS));
    assertFalse(fileStore.supportsFileAttributeView(FileAttributeViews.OWNER));
    assertFalse(fileStore.supportsFileAttributeView(FileAttributeViews.USER));
    assertFalse(fileStore.supportsFileAttributeView(FileAttributeViews.ACL));
  }

}

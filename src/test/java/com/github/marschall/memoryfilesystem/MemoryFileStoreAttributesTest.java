package com.github.marschall.memoryfilesystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class MemoryFileStoreAttributesTest {

  @RegisterExtension
  final FileStoreExtension extension = new FileStoreExtension();

  @Test
  void readsSpaceAttributesFromPublicFileStore() throws IOException {
    FileStore store = this.extension.getFileStore();
    assertEquals(store.getTotalSpace(), store.getAttribute("totalSpace"));
    // Free memory can change between calls, so do not compare two samples.
    assertThat((Long) store.getAttribute("usableSpace")).isPositive().isLessThanOrEqualTo(store.getTotalSpace());
    assertThat((Long) store.getAttribute("unallocatedSpace")).isPositive().isLessThanOrEqualTo(store.getTotalSpace());
  }

  @Test
  void delegatesToTheExistingSpaceAccessors() {
    MemoryFileStore store = new MemoryFileStore("space-values", new ClosedFileSystemChecker()) {
      @Override
      public long getTotalSpace() {
        return 101L;
      }

      @Override
      public long getUsableSpace() {
        return 37L;
      }

      @Override
      public long getUnallocatedSpace() {
        return 53L;
      }
    };
    assertEquals(101L, store.getAttribute("totalSpace"));
    assertEquals(37L, store.getAttribute("usableSpace"));
    assertEquals(53L, store.getAttribute("unallocatedSpace"));
  }

  @Test
  void rejectsUnsupportedAttributeNames() {
    FileStore store = this.extension.getFileStore();
    for (String attribute : new String[] {null, "", "unknown", "TotalSpace", "basic:totalSpace"}) {
      assertThrows(UnsupportedOperationException.class, () -> store.getAttribute(attribute));
    }
  }

  @Test
  void checksClosedFileSystemBeforeReadingAttributes() throws IOException {
    FileStore store;
    try (FileSystem fileSystem = MemoryFileSystemBuilder.newEmpty().build("closed-store-attributes")) {
      store = fileSystem.getFileStores().iterator().next();
    }
    for (String attribute : new String[] {"totalSpace", "usableSpace", "unallocatedSpace", "unknown", null}) {
      assertThrows(ClosedFileSystemException.class, () -> store.getAttribute(attribute));
    }
  }

}

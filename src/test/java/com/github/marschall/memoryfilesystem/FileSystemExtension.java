package com.github.marschall.memoryfilesystem;

import java.nio.file.FileSystem;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

class FileSystemExtension implements BeforeEachCallback, AfterEachCallback {

  static final String FILE_SYSTEM_NAME = "name";

  private FileSystem fileSystem;

  FileSystem getFileSystem() {
    return this.fileSystem;
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    this.fileSystem = MemoryFileSystemBuilder.newEmpty().build(FILE_SYSTEM_NAME);
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    if (this.fileSystem != null) {
      this.fileSystem.close();
    }
  }
}

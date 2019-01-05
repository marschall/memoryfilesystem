package com.github.marschall.memoryfilesystem;

import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import static com.github.marschall.memoryfilesystem.Constants.SAMPLE_ENV;
import static com.github.marschall.memoryfilesystem.Constants.SAMPLE_URI;

public class FileStoreExtension implements BeforeEachCallback, AfterEachCallback {

  private FileStore fileStore;
  private FileSystem fileSystem;


  FileStore getFileStore() {
    return this.fileStore;
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    this.fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV);
    this.fileStore = this.fileSystem.getFileStores().iterator().next();
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    if (this.fileSystem != null) {
      this.fileSystem.close();
    }
  }
}

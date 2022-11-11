package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.Constants.SAMPLE_ENV;
import static com.github.marschall.memoryfilesystem.Constants.SAMPLE_URI;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

class RootExtension implements BeforeEachCallback, AfterEachCallback {

  private Path root;
  private FileSystem fileSystem;


  Path getRoot() {
    return this.root;
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    this.fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV);
    this.root = fileSystem.getRootDirectories().iterator().next();
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    if (this.fileSystem != null) {
      this.fileSystem.close();
    }
  }
}

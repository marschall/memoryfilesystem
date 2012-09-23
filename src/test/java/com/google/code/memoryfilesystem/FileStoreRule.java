package com.google.code.memoryfilesystem;

import static com.google.code.memoryfilesystem.Constants.SAMPLE_ENV;
import static com.google.code.memoryfilesystem.Constants.SAMPLE_URI;

import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

final class FileStoreRule implements TestRule {

  private FileStore fileStore;


  FileStore getFileStore() {
    return fileStore;
  }

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {

      @Override
      public void evaluate() throws Throwable {
        try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
          fileStore = getFileStore(fileSystem);
          base.evaluate();
        }
      }

      private FileStore getFileStore(FileSystem fileSystem) {
        return fileSystem.getFileStores().iterator().next();
      }

    };
  }

}

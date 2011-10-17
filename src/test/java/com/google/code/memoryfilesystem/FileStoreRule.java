package com.google.code.memoryfilesystem;

import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import static com.google.code.memoryfilesystem.Constants.*;

class FileStoreRule implements MethodRule {

  private FileStore fileStore;


  FileStore getFileStore() {
    return fileStore;
  }



  @Override
  public Statement apply(final Statement base, FrameworkMethod method, Object target) {
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

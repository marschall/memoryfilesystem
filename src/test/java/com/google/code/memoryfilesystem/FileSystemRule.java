package com.google.code.memoryfilesystem;

import static com.google.code.memoryfilesystem.Constants.SAMPLE_ENV;
import static com.google.code.memoryfilesystem.Constants.SAMPLE_URI;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

final class FileSystemRule implements TestRule {
  
  private FileSystem fileSystem;


  FileSystem getFileSystem() {
    return fileSystem;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {

      /**
       * {@inheritDoc}
       */
      @Override
      public void evaluate() throws Throwable {
        fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV);
//        fileSystem = FileSystems.getDefault();
                
        try {
          base.evaluate();
        } finally {
          fileSystem.close();
        }
      }

    };
  }

}

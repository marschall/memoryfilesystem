package com.google.code.memoryfilesystem;

import static com.google.code.memoryfilesystem.Constants.SAMPLE_ENV;
import static com.google.code.memoryfilesystem.Constants.SAMPLE_URI;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

final class RootRule implements TestRule {

  private Path root;


  Path getRoot() {
    return root;
  }


  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {

      /**
       * {@inheritDoc}
       */
      @Override
      public void evaluate() throws Throwable {
        try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
          root = getRoot(fileSystem);
          base.evaluate();
        }
      }

      private Path getRoot(FileSystem fileSystem) {
        return fileSystem.getRootDirectories().iterator().next();
      }

    };
  }

}

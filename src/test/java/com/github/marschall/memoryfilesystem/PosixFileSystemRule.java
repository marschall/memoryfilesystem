package com.github.marschall.memoryfilesystem;

import java.nio.file.FileSystem;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

final class PosixFileSystemRule implements TestRule {

  private FileSystem fileSystem;


  FileSystem getFileSystem() {
    return this.fileSystem;
  }


  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {

      @Override
      public void evaluate() throws Throwable {
        PosixFileSystemRule.this.fileSystem = MemoryFileSystemBuilder.newLinux().build("name");

        try {
          base.evaluate();
        } finally {
          PosixFileSystemRule.this.fileSystem.close();
        }
      }

    };
  }

}

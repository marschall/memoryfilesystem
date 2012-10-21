package com.github.marschall.memoryfilesystem;

import java.nio.file.FileSystem;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

final class UnixFileSystemRule implements TestRule {

  private FileSystem fileSystem;


  FileSystem getFileSystem() {
    return this.fileSystem;
  }


  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {

      @Override
      public void evaluate() throws Throwable {
        UnixFileSystemRule.this.fileSystem = MemoryFileSystemBuilder.newUnix().build("name");

        try {
          base.evaluate();
        } finally {
          UnixFileSystemRule.this.fileSystem.close();
        }
      }

    };
  }

}

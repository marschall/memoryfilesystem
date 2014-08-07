package com.github.marschall.memoryfilesystem;

import java.nio.file.FileSystem;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

final class WindowsFileSystemRule implements TestRule {

  private FileSystem fileSystem;


  FileSystem getFileSystem() {
    return this.fileSystem;
  }


  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {

      @Override
      public void evaluate() throws Throwable {
        WindowsFileSystemRule.this.fileSystem = MemoryFileSystemBuilder.newWindows().build("WindowsFileSystemRule");

        try {
          base.evaluate();
        } finally {
          WindowsFileSystemRule.this.fileSystem.close();
        }
      }

    };
  }

}

package com.github.marschall.memoryfilesystem;

import java.nio.file.FileSystem;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class MacOsFileSystemRule implements TestRule {

  private FileSystem fileSystem;


  FileSystem getFileSystem() {
    return this.fileSystem;
  }


  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {

      @Override
      public void evaluate() throws Throwable {
        MacOsFileSystemRule.this.fileSystem = MemoryFileSystemBuilder.newMacOs().build("MacOsFileSystemRule");

        try {
          base.evaluate();
        } finally {
          MacOsFileSystemRule.this.fileSystem.close();
        }
      }

    };
  }

}

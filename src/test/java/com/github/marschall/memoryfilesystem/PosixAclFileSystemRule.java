package com.github.marschall.memoryfilesystem;

import java.nio.file.FileSystem;
import java.nio.file.attribute.AclFileAttributeView;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

final class PosixAclFileSystemRule implements TestRule {

  private FileSystem fileSystem;


  FileSystem getFileSystem() {
    return this.fileSystem;
  }


  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {

      @Override
      public void evaluate() throws Throwable {
        PosixAclFileSystemRule.this.fileSystem = MemoryFileSystemBuilder.newLinux()
                .addFileAttributeView(AclFileAttributeView.class)
                .build("PosixAclFileSystemRule");

        try {
          base.evaluate();
        } finally {
          PosixAclFileSystemRule.this.fileSystem.close();
        }
      }

    };
  }

}

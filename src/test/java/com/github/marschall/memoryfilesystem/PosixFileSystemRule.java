package com.github.marschall.memoryfilesystem;

import java.nio.file.FileSystem;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

final class PosixFileSystemRule implements TestRule {

  private FileSystem fileSystem;

  private final Set<PosixFilePermission> umask;

  PosixFileSystemRule(Set<PosixFilePermission> umask) {
    this.umask = umask;
  }

  PosixFileSystemRule() {
    this(EnumSet.noneOf(PosixFilePermission.class));
  }


  FileSystem getFileSystem() {
    return this.fileSystem;
  }


  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {

      @Override
      public void evaluate() throws Throwable {
        PosixFileSystemRule.this.fileSystem = MemoryFileSystemBuilder.newLinux()
                .setUmask(PosixFileSystemRule.this.umask)
                .build("PosixFileSystemRule");

        try {
          base.evaluate();
        } finally {
          PosixFileSystemRule.this.fileSystem.close();
        }
      }

    };
  }

}

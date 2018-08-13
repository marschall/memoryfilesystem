package com.github.marschall.memoryfilesystem;

import java.nio.file.FileSystem;
import java.nio.file.attribute.PosixFileAttributeView;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

final class PosixPermissionFileSystemRule implements TestRule {

  static final String OWNER = "owner";
  static final String GROUP = "group";
  static final String OTHER = "other";

  private FileSystem fileSystem;


  FileSystem getFileSystem() {
    return this.fileSystem;
  }


  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {

      @Override
      public void evaluate() throws Throwable {
        PosixPermissionFileSystemRule.this.fileSystem = MemoryFileSystemBuilder.newEmpty()
                .addRoot(MemoryFileSystemProperties.UNIX_ROOT)
                .setSeparator(MemoryFileSystemProperties.UNIX_SEPARATOR)
                .addUser(OWNER)
                .addGroup(OWNER)
                .addUser(GROUP)
                .addGroup(GROUP)
                .addUser(OTHER)
                .addGroup(OTHER)
                .addFileAttributeView(PosixFileAttributeView.class)
                .setCurrentWorkingDirectory("/home/" + OWNER)
                .setStoreTransformer(StringTransformers.IDENTIY)
                .setCaseSensitive(true)
                .addForbiddenCharacter((char) 0)
                .build("PosixPermissionFileSystemRule");

        try {
          base.evaluate();
        } finally {
          PosixPermissionFileSystemRule.this.fileSystem.close();
        }
      }

    };
  }

}

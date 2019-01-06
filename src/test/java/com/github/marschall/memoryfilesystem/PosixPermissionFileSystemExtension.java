package com.github.marschall.memoryfilesystem;

import java.nio.file.FileSystem;
import java.nio.file.attribute.PosixFileAttributeView;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

class PosixPermissionFileSystemExtension implements BeforeEachCallback, AfterEachCallback {

  static final String OWNER = "owner";
  static final String GROUP = "group";
  static final String OTHER = "other";

  private FileSystem fileSystem;

  FileSystem getFileSystem() {
    return this.fileSystem;
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    this.fileSystem = MemoryFileSystemBuilder.newEmpty()
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
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    if (this.fileSystem != null) {
      this.fileSystem.close();
    }
  }
}

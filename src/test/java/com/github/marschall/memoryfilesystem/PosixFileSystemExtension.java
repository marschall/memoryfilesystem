package com.github.marschall.memoryfilesystem;

import java.nio.file.FileSystem;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class PosixFileSystemExtension implements BeforeEachCallback, AfterEachCallback {

  private FileSystem fileSystem;
  private final Set<PosixFilePermission> umask;

  PosixFileSystemExtension(Set<PosixFilePermission> umask) {
    this.umask = umask;
  }

  PosixFileSystemExtension() {
    this(EnumSet.noneOf(PosixFilePermission.class));
  }


  FileSystem getFileSystem() {
    return this.fileSystem;
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    this.fileSystem = MemoryFileSystemBuilder.newLinux()
        .setUmask(this.umask)
        .build("PosixFileSystemRule");
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    if (this.fileSystem != null) {
      this.fileSystem.close();
    }
  }
}

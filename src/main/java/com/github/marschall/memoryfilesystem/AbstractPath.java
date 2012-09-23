package com.github.marschall.memoryfilesystem;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;

abstract class AbstractPath implements Path {

  private final MemoryFileSystem fileSystem;

  AbstractPath(MemoryFileSystem fileSystem) {
    this.fileSystem = fileSystem;
  }


  @Override
  public FileSystem getFileSystem() {
    return this.fileSystem;
  }

  MemoryFileSystem getMemoryFileSystem() {
    return this.fileSystem;
  }



  @Override
  public File toFile() {
    throw new UnsupportedOperationException("memory file system does not support #toFile()");
  }



  @Override
  public final boolean startsWith(Path other) {
    if (!this.isSameFileSystem(other)) {
      return false;
    }
    return startsWith((AbstractPath) other);
  }

  abstract boolean startsWith(AbstractPath other);



  @Override
  public final boolean endsWith(Path other) {
    if (!this.isSameFileSystem(other)) {
      return false;
    }
    return endsWith((AbstractPath) other);
  }

  abstract boolean endsWith(AbstractPath other);


  @Override
  public final Path resolve(Path other) {
    assertSameFileSystem(other);
    return resolve((AbstractPath) other);
  }

  abstract Path resolve(AbstractPath other);


  @Override
  public final Path resolveSibling(Path other) {
    assertSameFileSystem(other);
    return resolveSibling((AbstractPath) other);
  }

  abstract Path resolveSibling(AbstractPath other);



  @Override
  public final Path relativize(Path other) {
    assertSameFileSystem(other);
    return relativize((AbstractPath) other);
  }

  abstract Path relativize(AbstractPath other);
  
  private boolean isSameFileSystem(Path other) {
    return other.getFileSystem() == this.getFileSystem();
  }

  private void assertSameFileSystem(Path other) {
    if (!this.isSameFileSystem(other)) {
      throw new ProviderMismatchException();
    }
  }


}

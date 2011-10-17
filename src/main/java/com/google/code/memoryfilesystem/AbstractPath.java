package com.google.code.memoryfilesystem;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;

abstract class AbstractPath implements Path {

  private final MemoryFileSystem fileSystem;

  AbstractPath(MemoryFileSystem fileSystem) {
    this.fileSystem = fileSystem;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FileSystem getFileSystem() {
    return this.fileSystem;
  }

  MemoryFileSystem getMemoryFileSystem() {
    return this.fileSystem;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public File toFile() {
    throw new UnsupportedOperationException("memory file system does not support #toFile()");
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public final boolean startsWith(Path other) {
    assertSameProvider(other);
    return startsWith((AbstractPath) other);
  }

  abstract boolean startsWith(AbstractPath other);


  /**
   * {@inheritDoc}
   */
  @Override
  public final boolean endsWith(Path other) {
    assertSameProvider(other);
    return endsWith((AbstractPath) other);
  }

  abstract boolean endsWith(AbstractPath other);


  /**
   * {@inheritDoc}
   */
  @Override
  public final Path resolve(Path other) {
    assertSameProvider(other);
    return resolve((AbstractPath) other);
  }

  abstract Path resolve(AbstractPath other);

  /**
   * {@inheritDoc}
   */
  @Override
  public final Path resolveSibling(Path other) {
    assertSameProvider(other);
    return resolveSibling((AbstractPath) other);
  }

  abstract Path resolveSibling(AbstractPath other);


  /**
   * {@inheritDoc}
   */
  @Override
  public final Path relativize(Path other) {
    assertSameProvider(other);
    return relativize((AbstractPath) other);
  }

  abstract Path relativize(AbstractPath other);

  private void assertSameProvider(Path other) {
    if (!(other.getFileSystem() instanceof MemoryFileSystem)) {
      throw new ProviderMismatchException();
    }
  }


}

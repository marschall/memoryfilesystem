package com.github.marschall.memoryfilesystem;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.List;

abstract class AbstractPath implements Path {

  // TODO think about #isEmpty to replace the instanceof checks
  // TODO think about a visitor (visitRelative visitEmpty visitRoot) to replace instanceof checks

  private final MemoryFileSystem fileSystem;

  AbstractPath(MemoryFileSystem fileSystem) {
    this.fileSystem = fileSystem;
  }

  static AbstractPath createAboslute(MemoryFileSystem fileSystem, Root root, List<String> nameElements) {
    if (nameElements.isEmpty()) {
      return root;
    } else {
      return new AbsolutePath(fileSystem, root, nameElements);
    }
  }

  static AbstractPath createAboslute(MemoryFileSystem fileSystem, Root root, String nameElement) {
    return createAboslute(fileSystem, root, Collections.singletonList(nameElement));
  }

  static AbstractPath createRelative(MemoryFileSystem fileSystem, List<String> nameElements) {
    int nameElementCount = nameElements.size();
    if (nameElementCount == 0) {
      return fileSystem.getEmptyPath();
    } else if (nameElementCount == 1) {
      return createRelative(fileSystem, nameElements.get(0));
    } else {
      return new RelativePath(fileSystem, nameElements);
    }
  }

  static AbstractPath createRelative(MemoryFileSystem fileSystem, String nameElement) {
    return new SingletonPath(fileSystem, nameElement);
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
    return this.startsWith((AbstractPath) other);
  }

  abstract boolean startsWith(AbstractPath other);



  @Override
  public final boolean endsWith(Path other) {
    if (!this.isSameFileSystem(other)) {
      return false;
    }
    return this.endsWith((AbstractPath) other);
  }

  abstract boolean endsWith(AbstractPath other);


  @Override
  public final Path resolve(Path other) {
    this.assertSameFileSystem(other);
    AbstractPath otherPath = (AbstractPath) other;
    if (otherPath.isRoot()) {
      return other;
    } else if (other.isAbsolute()) {
      // TODO totally unspecified, make configurable
      return other;
    } else if (otherPath.getNameCount() == 0) {
      return this;
    }
    return this.resolve((ElementPath) otherPath);
  }

  abstract Path resolve(ElementPath other);


  @Override
  public final Path resolveSibling(Path other) {
    this.assertSameFileSystem(other);
    return this.resolveSibling((AbstractPath) other);
  }

  abstract Path resolveSibling(AbstractPath other);

  abstract boolean isRoot();

  @Override
  public Path resolve(String other) {
    return this.resolve(this.fileSystem.getPath(other));
  }

  @Override
  public Path resolveSibling(String other) {
    return this.resolveSibling(this.fileSystem.getPath(other));
  }


  @Override
  public final Path relativize(Path other) {
    this.assertSameFileSystem(other);
    return this.relativize((AbstractPath) other);
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

  @Override
  public int compareTo(Path other) {
    if (this == other) {
      return 0;
    }

    FileSystemProvider thisProvider = this.getFileSystem().provider();
    FileSystemProvider otherProvider = other.getFileSystem().provider();
    if (thisProvider != otherProvider) {
      String message = this + " can only be compared to paths of provider: " + thisProvider
              + " but " + other + " had provider: " + otherProvider;
      throw new ClassCastException(message);
    }
    AbstractPath otherPath = (AbstractPath) other;
    String otherFileSystemKey = otherPath.getMemoryFileSystem().getKey();
    String thisFileSystemKey = this.getMemoryFileSystem().getKey();
    // always take case into account
    int fileSystemComparison = thisFileSystemKey.compareTo(otherFileSystemKey);
    if (fileSystemComparison != 0) {
      return fileSystemComparison;
    }
    return this.compareTo(otherPath);
  }

  abstract int compareTo(AbstractPath other);


}

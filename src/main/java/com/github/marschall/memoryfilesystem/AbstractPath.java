package com.github.marschall.memoryfilesystem;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

abstract class AbstractPath implements Path {

  // TODO think about #isEmpty to replace the instanceof checks
  // TODO think about a visitor (visitRelative visitEmpty visitRoot) to replace instanceof checks

  private final MemoryFileSystem fileSystem;

  AbstractPath(MemoryFileSystem fileSystem) {
    this.fileSystem = fileSystem;
  }

  static AbstractPath createAboslute(MemoryFileSystem fileSystem, Root root, List<String> nameElements) {
    if (root == null) {
      throw new IllegalArgumentException("root must not be null");
    }
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
  public MemoryFileSystem getFileSystem() {
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

    MemoryFileSystemProvider otherProvider = (MemoryFileSystemProvider) other.getFileSystem().provider();
    if (otherProvider == null) {
      // can't happen, just there to shut up compiler about unsed variable
      throw new ClassCastException("no file system provider given");
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

  @Override
  public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) {
    // TODO report bug
    // TODO java.nio.file.NotDirectoryException
    if (true) {
      throw new UnsupportedOperationException();
    }
    if (modifiers != null && modifiers.length > 0) {
      throw new UnsupportedOperationException("no modifiers supported");
    }
    if (watcher == null) {
      throw new NullPointerException("watcher is null");
    }
    // triggers class cast exception but this is API in other cases
    MemoryFileSystemWatchService memoryWatcher = (MemoryFileSystemWatchService) watcher;
    if (memoryWatcher.getMemoryFileSystem() != this.fileSystem) {
      throw new IllegalArgumentException("watcher has to be from the same file system");
    }
    MemoryWatchKey watchKey = new MemoryWatchKey(this, memoryWatcher, this.asSet(events));
    this.fileSystem.register(watchKey);
    return watchKey;
  }

  private Set<Kind<?>> asSet(Kind<?>[] eventKinds) {
    if (eventKinds == null) {
      throw new NullPointerException("watcher is null");
    }
    int numberOfKinds = eventKinds.length;
    if (numberOfKinds == 0) {
      throw new IllegalArgumentException("no events specified");
    }
    if (numberOfKinds == 1) {
      Kind<?> eventKind = eventKinds[0];
      this.validate(eventKind);
      return Collections.<Kind<?>>singleton(eventKind);
    } else {
      Set<Kind<?>> set = new HashSet<>(numberOfKinds);
      for (Kind<?> kind : eventKinds) {
        this.validate(kind);
        set.add(kind);
      }
      return set;
    }
  }

  private void validate(Kind<?> eventKind) {
    if (eventKind != ENTRY_CREATE && eventKind != ENTRY_DELETE && eventKind != ENTRY_MODIFY) {
      throw new UnsupportedOperationException("unsupported event kind: " + eventKind);
    }
  }


  @Override
  public WatchKey register(WatchService watcher, Kind<?>... events) throws IOException {
    return this.register(watcher, events, (Modifier[]) null);
  }


}

  package com.google.code.memoryfilesystem;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

abstract class ElementPath extends AbstractPath {

  private final List<String> nameElements;

  ElementPath(MemoryFileSystem fileSystem, List<String> nameElements) {
    super(fileSystem);
    this.nameElements = nameElements;
  }
  
  List<String> getNameElements() {
    return nameElements;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path getFileName() {
    if (this.nameElements.isEmpty()) {
      //REVIEW can this really happen?
      return null;
    } else {
      String lastElements = nameElements.get(nameElements.size() - 1);
      return new RelativePath(getMemoryFileSystem(), Collections.singletonList(lastElements));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getNameCount() {
    return this.nameElements.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean startsWith(String other) {
    Path path = this.getMemoryFileSystem().getPath(other);
    return this.startsWith(path);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean endsWith(String other) {
    Path path = this.getMemoryFileSystem().getPath(other);
    return this.endsWith(path);
  }

}
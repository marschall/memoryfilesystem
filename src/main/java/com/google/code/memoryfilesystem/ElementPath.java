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

}
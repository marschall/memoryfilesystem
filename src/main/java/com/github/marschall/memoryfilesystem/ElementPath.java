  package com.github.marschall.memoryfilesystem;

import java.util.List;

abstract class ElementPath extends AbstractPath {
  

  ElementPath(MemoryFileSystem fileSystem) {
    super(fileSystem);
  }
  
  abstract List<String> getNameElements();
  
  abstract String getNameElement(int index);
  
  abstract String getLastNameElement();


}
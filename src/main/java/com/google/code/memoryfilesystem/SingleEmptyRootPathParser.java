package com.google.code.memoryfilesystem;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class SingleEmptyRootPathParser implements PathParser {

  /**
   * {@inheritDoc}
   */
  @Override
  public Path parse(Iterable<Root> roots, String first, String... more) {
    // REVIEW implement #count() to correctly set initial size
    // TODO check for empty path
    List<String> elements = new ArrayList<>();
    this.parseInto(first, elements);
    if (more != null && more.length > 0) {
      for (String s : more) {
        this.parseInto(s, elements);
      }
    }
    Root root = roots.iterator().next();
    MemoryFileSystem memoryFileSystem = root.getMemoryFileSystem();
    if (this.isAbsolute(first, more)) {
      if (elements.isEmpty()) {
        return root;
      } else {
        return new AbsolutePath(memoryFileSystem, root, elements);
      }
    } else {
      return new RelativePath(memoryFileSystem, elements);
    }
  }
  
  private boolean isAbsolute(String first, String... more) {
    if (!first.isEmpty()) {
      return first.charAt(0) == '/';
    }
    if (more != null && more.length > 0) {
      for (String s : more) {
        if (!s.isEmpty()) {
          return s.charAt(0) == '/';
        }
      }
    }
    
    // only empty strings
    return false;
  }
  
  private void parseInto(String s, List<String> elements) {
    if (s.isEmpty()) {
      return;
    }
    
    int fromIndex = 0;
    int slashIndex = s.indexOf('/', fromIndex);
    while (slashIndex != -1) {
      if (slashIndex > fromIndex) {
        // avoid empty strings for things like //
        elements.add(s.substring(fromIndex, slashIndex));
      }
      
      fromIndex = slashIndex + 1;
      slashIndex  = s.indexOf('/', fromIndex);
    }
    if (fromIndex < s.length()) {
      elements.add(s.substring(fromIndex));
    }
    
  }

}

package com.github.marschall.memoryfilesystem;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class SingleEmptyRootPathParser extends PathParser {

  SingleEmptyRootPathParser(String separator) {
    super(separator);
  }

  @Override
  public Path parse(Map<String, Root> roots, String first, String... more) {
    List<String> elements = new ArrayList<>(count(first, more));
    this.parseInto(first, elements);
    if (more != null && more.length > 0) {
      for (String s : more) {
        this.parseInto(s, elements);
      }
    }
    Root root = roots.values().iterator().next();
    MemoryFileSystem memoryFileSystem = root.getMemoryFileSystem();
    if (this.startWithSeparator(first, more)) {
      return AbstractPath.createAboslute(memoryFileSystem, root, elements);
    } else {
      return AbstractPath.createRelative(memoryFileSystem, elements);
    }
  }

  static int count(String first, String... more) {
    int count = count(first);
    if (more != null && more.length > 0) {
      for (String s : more) {
        count += count(s);
      }
    }
    return count;
  }

  static int count(String s) {
    if (s.isEmpty()) {
      return 0;
    }
    int count = 0;

    int fromIndex = 0;
    int slashIndex = s.indexOf('/', fromIndex);

    while (slashIndex != -1) {
      if (slashIndex > fromIndex) {
        // avoid empty strings for things like //
        count += 1;
      }

      fromIndex = slashIndex + 1;
      slashIndex  = s.indexOf('/', fromIndex);
    }
    if (fromIndex < s.length()) {
      count += 1;
    }

    return count;
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

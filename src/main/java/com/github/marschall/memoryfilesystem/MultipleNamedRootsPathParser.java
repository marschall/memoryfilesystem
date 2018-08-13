package com.github.marschall.memoryfilesystem;

import static java.lang.Math.min;

import java.nio.file.InvalidPathException;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class MultipleNamedRootsPathParser extends PathParser {

  private final StringTransformer pathTransformer;


  MultipleNamedRootsPathParser(String separator, StringTransformer pathTransformer, CharacterSet forbiddenCharacters) {
    super(separator, forbiddenCharacters);
    this.pathTransformer = pathTransformer;
  }

  @Override
  AbstractPath parseUri(Map<String, Root> rootByKey, String uri) {
    for (int i = 0; i < uri.length(); ++i) {
      if (uri.charAt(i) != '/') {
        return this.parse(rootByKey, uri.substring(i), EMPTY);
      }
    }
    throw new InvalidPathException(uri, "not a valid path: ");
  }

  @Override
  public AbstractPath parse(Map<String, Root> roots, String first, String... more) {
    if (this.startWithSeparator(first, more)) {
      // TODO build string
      throw new InvalidPathException(first, "path must not start with separator", 1);
    }

    // REVIEW implement #count() to correctly set initial size
    List<String> elements = new ArrayList<>();
    this.parseInto(first, elements);
    if (more != null && more.length > 0) {
      for (String s : more) {
        this.parseInto(s, elements);
      }
    }

    MemoryFileSystem memoryFileSystem = this.getFileSystem(roots);
    if (this.isAbsolute(elements)) {
      Root root = this.getRoot(memoryFileSystem, roots, elements);
      elements = elements.subList(1, elements.size());
      this.check(elements);
      return AbstractPath.createAbsolute(memoryFileSystem, root, elements);
    } else {
      this.check(elements);
      return AbstractPath.createRelative(memoryFileSystem, elements);
    }
  }

  @Override
  PathMatcher parseGlob(String pattern) {
    if (this.startWithSeparator(pattern)) {
      // TODO build string
      throw new InvalidPathException(pattern, "path must not start with separator", 1);
    }

    // REVIEW implement #count() to correctly set initial size
    List<String> elements = new ArrayList<>();
    this.parseInto(pattern, elements);

    if (this.isAbsolute(elements)) {
      elements = elements.subList(1, elements.size());
      return new GlobPathMatcher(true, convertToPatterns(elements));
    } else {
      return new GlobPathMatcher(false, convertToPatterns(elements));
    }
  }

  private MemoryFileSystem getFileSystem(Map<String, Root> roots) {
    return roots.values().iterator().next().getMemoryFileSystem();
  }

  private Root getRoot(MemoryFileSystem memoryFileSystem, Map<String, Root> roots, List<String> elements) {
    String first = elements.get(0);
    String key = this.pathTransformer.transform(first.substring(0, 1)); // C: -> C
    Root root = roots.get(key);
    if (root != null) {
      return root;
    } else {
      // create a fake root that is not really in the file system
      return new NamedRoot(memoryFileSystem, key);
    }
  }

  private boolean isAbsolute(List<String> elements) {
    if (elements.isEmpty()) {
      return false;
    }
    String first = elements.get(0);
    return first.length() == 2
            && first.charAt(1) == ':';
  }

  @Override
  boolean isAbsolute(String path) {
    return path.length() >= 2
            && path.charAt(1) == ':';
  }


  private void parseInto(String s, List<String> elements) {
    if (s.isEmpty()) {
      return;
    }

    int fromIndex = 0;
    int slashIndex = s.indexOf('/', fromIndex);
    int separatorIndex = s.indexOf(this.separator, fromIndex);
    int nextIndex = this.computeNextIndex(slashIndex, separatorIndex);

    while (nextIndex != -1) {
      if (nextIndex > fromIndex) {
        // avoid empty strings for things like //
        elements.add(s.substring(fromIndex, nextIndex));
      }

      fromIndex = nextIndex + 1;
      if (slashIndex == -1) {
        slashIndex = s.indexOf(this.separator, separatorIndex + 1);
        nextIndex = slashIndex;
      } else if (separatorIndex == -1) {
        separatorIndex = s.indexOf('/', slashIndex + 1);
        nextIndex = separatorIndex;
      } else {
        if (slashIndex < separatorIndex) {
          slashIndex = s.indexOf('/', slashIndex + 1);
        } else {
          // they can not be equal
          separatorIndex = s.indexOf(this.separator, separatorIndex + 1);
        }
        nextIndex = this.computeNextIndex(slashIndex, separatorIndex);
      }
    }
    if (fromIndex < s.length()) {
      elements.add(s.substring(fromIndex));
    }
  }

  private int computeNextIndex(int slashIndex, int separatorIndex) {
    if (slashIndex == -1) {
      return separatorIndex;
    } else if (separatorIndex == -1) {
      return slashIndex;
    } else {
      return min(slashIndex, separatorIndex);
    }
  }

}

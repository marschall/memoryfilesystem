package com.github.marschall.memoryfilesystem;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Collections;
import java.util.List;

final class GlobPathMatcher implements PathMatcher {

  private final boolean isAbsolute;

  private final List<GlobPattern> patterns;

  GlobPathMatcher(boolean isAbsolute, List<GlobPattern> matches) {
    this.isAbsolute = isAbsolute;
    this.patterns = matches;
  }

  @Override
  public boolean matches(Path path) {
    if (this.isAbsolute && !path.isAbsolute()) {
      return false;
    }
    ElementPath elementPath = (ElementPath) path;
    return this.matches(elementPath.getNameElements(), this.patterns);
  }

  private boolean matches(List<String> elements, List<GlobPattern> patterns) {
    if (elements.isEmpty()) {
      for (GlobPattern pattern : patterns) {
        if (!pattern.isCrossingDirectoryBoundaries()) {
          return false;
        }
      }
      return true;
    }

    String element = elements.get(0);
    if (elements.size() == 1) {
      GlobPattern match = patterns.get(0);
      if (!match.matches(element)) {
        return false;
      } else if (patterns.size() == 1) {
        return true;
      } else {
        List<GlobPattern> remainingMatches = patterns.subList(1, patterns.size());
        return this.matches(Collections.<String>emptyList(), remainingMatches);
      }
    }

    if (patterns.isEmpty()) {
      return false;
    }

    GlobPattern firstMatch = patterns.get(0);
    if (!firstMatch.isCrossingDirectoryBoundaries()) {
      if (firstMatch.matches(element) && patterns.size() > 1) {
        return this.matches(elements.subList(1, elements.size()), patterns.subList(1, patterns.size()));
      } else {
        return false;
      }
    } else {
      List<String> remainingElements = elements.subList(1, elements.size());
      return this.matches(remainingElements, patterns)
              || this.matches(remainingElements, patterns.subList(1, patterns.size()));
    }

  }

  static String name() {
    return "glob";
  }

  interface GlobPattern {

    boolean isCrossingDirectoryBoundaries();

    boolean matches(String element);

  }


}

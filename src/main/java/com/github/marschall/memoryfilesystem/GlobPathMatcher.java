package com.github.marschall.memoryfilesystem;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Collections;
import java.util.List;

final class GlobPathMatcher implements PathMatcher {

  private final boolean isAbsolute;

  private final List<GlobMatch> matches;

  GlobPathMatcher(boolean isAbsolute, List<GlobMatch> matches) {
    this.isAbsolute = isAbsolute;
    this.matches = matches;
  }

  @Override
  public boolean matches(Path path) {
    if (path.isAbsolute() != this.isAbsolute) {
      return false;
    }
    ElementPath elementPath = (ElementPath) path;
    return this.matches(elementPath.getNameElements(), this.matches);
  }

  private boolean matches(List<String> elements, List<GlobMatch> matches) {
    if (elements.isEmpty()) {
      for (GlobMatch match : matches) {
        if (!match.isFlexible()) {
          return false;
        }
      }
      return true;
    }

    String element = elements.get(0);
    if (elements.size() == 1) {
      for (int i = 0; i < matches.size(); ++i) {
        GlobMatch match = matches.get(i);
        if (!match.isFlexible()) {
          if (!match.matches(element)) {
            return false;
          } else if (i == matches.size() - 1) {
            return true;
          } else {
            List<GlobMatch> remainingMatches = matches.subList(i + 1, matches.size());
            return this.matches(Collections.<String>emptyList(), remainingMatches);
          }
        }
      }
    }

    if (matches.isEmpty()) {
      return false;
    }

    GlobMatch firstMatch = matches.get(0);
    if (!firstMatch.isFlexible()) {
      if (firstMatch.matches(element) && matches.size() > 1) {
        return this.matches(elements.subList(1, elements.size()), matches.subList(1, matches.size()));
      } else {
        return false;
      }
    } else {
      List<String> remainingElements = elements.subList(1, elements.size());
      return this.matches(remainingElements, matches)
              || this.matches(remainingElements, matches.subList(1, matches.size()));
    }

  }

  static String name() {
    return "glob";
  }

  interface GlobMatch {

    boolean isFlexible();

    boolean matches(String element);

  }


}

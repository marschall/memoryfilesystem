package com.github.marschall.memoryfilesystem;

import java.nio.file.InvalidPathException;
import java.util.List;
import java.util.Map;

abstract class PathParser {

  final char separator;
  private final CharacterSet forbiddenCharacters;

  PathParser(String separator, CharacterSet forbiddenCharacters) {
    this.forbiddenCharacters = forbiddenCharacters;
    if (separator.length() != 1) {
      throw new IllegalArgumentException("separator must have length 1 but was \"" + separator + "\"");
    }
    this.separator = separator.charAt(0);
  }

  void check(List<String> elements) {
    for (String element : elements) {
      if (this.forbiddenCharacters.containsAny(element)) {
        throw new InvalidPathException(element, "contains a not allowed character");
      }
    }
  }


  boolean startWithSeparator(String s) {
    return s.charAt(0) == '/' || s.charAt(0) == this.separator;
  }

  abstract AbstractPath parse(Map<String, Root> roots, String first, String... more);


  boolean startWithSeparator(String first, String... more) {
    if (!first.isEmpty()) {
      return this.startWithSeparator(first);
    }
    if (more != null && more.length > 0) {
      for (String s : more) {
        if (!s.isEmpty()) {
          return this.startWithSeparator(s);
        }
      }
    }

    // only empty strings
    return false;
  }

}

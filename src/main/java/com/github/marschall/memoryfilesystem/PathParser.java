package com.github.marschall.memoryfilesystem;

import java.util.Map;

abstract class PathParser {

  final char separator;

  PathParser(String separator) {
    if (separator.length() != 1) {
      throw new IllegalArgumentException("separator must have length 1 but was \"" + separator + "\"");
    }
    this.separator = separator.charAt(0);
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

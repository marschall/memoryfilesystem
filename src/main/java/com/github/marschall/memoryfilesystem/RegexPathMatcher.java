package com.github.marschall.memoryfilesystem;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.regex.Pattern;

final class RegexPathMatcher implements PathMatcher {

  private final Pattern pattern;

  RegexPathMatcher(Pattern pattern) {
    this.pattern = pattern;
  }

  @Override
  public boolean matches(Path path) {
    return this.pattern.matcher(path.toString()).matches();
  }
  
  static String name() {
    return "regex";
  }

}

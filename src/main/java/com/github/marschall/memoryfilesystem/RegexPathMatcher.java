package com.github.marschall.memoryfilesystem;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.regex.Pattern;

abstract class RegexPathMatcher implements PathMatcher {

  final Pattern pattern;

  RegexPathMatcher(Pattern pattern) {
    this.pattern = pattern;
  }

  static String name() {
    return "regex";
  }

}

final class RegexAbsolutePathMatcher extends RegexPathMatcher {

  RegexAbsolutePathMatcher(Pattern pattern) {
    super(pattern);
  }

  @Override
  public boolean matches(Path path) {
    MemoryFileSystemProvider.castPath(path);
    return this.pattern.matcher(path.toAbsolutePath().toString()).matches();
  }

}

final class RegexRelativePathMatcher extends RegexPathMatcher {

  RegexRelativePathMatcher(Pattern pattern) {
    super(pattern);
  }

  @Override
  public boolean matches(Path path) {
    AbstractPath abstractPath = MemoryFileSystemProvider.castPath(path);
    if (path.isAbsolute()) {
      AbstractPath defaultPath = abstractPath.getMemoryFileSystem().getDefaultPath();
      path = defaultPath.relativize(path);
    }
    return this.pattern.matcher(path.toString()).matches();
  }

}

package com.github.marschall.memoryfilesystem;

import java.nio.file.Path;
import java.nio.file.PathMatcher;

final class GlobPathMatcher implements PathMatcher {

  private final String pattern;

  GlobPathMatcher(String pattern) {
    this.pattern = pattern;
  }

  @Override
  public boolean matches(Path path) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }
  
  static String name() {
    return "glob";
  }

}

package com.github.marschall.memoryfilesystem;

import java.nio.file.Path;
import java.nio.file.PathMatcher;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

class PathMatchesMatcher  extends TypeSafeMatcher<PathMatcher> {

  private final Path path;

  private PathMatchesMatcher(Path path) {
    this.path = path;
  }

  static Matcher<PathMatcher> matches(Path path) {
    return new PathMatchesMatcher(path);
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("matched the path");
    description.appendValue(this.path);
  }

  @Override
  protected boolean matchesSafely(PathMatcher item) {
    return item.matches(this.path);
  }

}

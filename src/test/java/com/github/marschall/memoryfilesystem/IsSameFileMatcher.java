package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

final class IsSameFileMatcher extends TypeSafeMatcher<Path> {

  private final Path path;

  private IsSameFileMatcher(Path path) {
    this.path = path;
  }

  static Matcher<Path> isSameFile(Path path) {
    return new IsSameFileMatcher(path);
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("is same path as").appendValue(this.path);
  }

  @Override
  protected boolean matchesSafely(Path item) {
    try {
      return Files.isSameFile(this.path, item);
    } catch (IOException e) {
      throw new RuntimeException("could not check for name path", e);
    }
  }

}

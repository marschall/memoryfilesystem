package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

final class IsHiddenMatcher extends TypeSafeMatcher<Path> {

  @Factory
  static Matcher<Path> isHidden() {
    return new IsHiddenMatcher();
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("file is hidden");
  }

  @Override
  protected boolean matchesSafely(Path path) {
    try {
      return Files.isHidden(path);
    } catch (IOException e) {
      throw new RuntimeException("could not check file: " + path, e);
    }
  }

}

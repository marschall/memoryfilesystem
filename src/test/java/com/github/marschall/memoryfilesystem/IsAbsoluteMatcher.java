package com.github.marschall.memoryfilesystem;

import java.nio.file.Path;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

final class IsAbsoluteMatcher extends TypeSafeMatcher<Path> {

  private static final TypeSafeMatcher<Path> INSTANCE = new IsAbsoluteMatcher();

  @Override
  public void describeTo(Description description) {
    description.appendText("is absolute");
  }

  @Override
  protected boolean matchesSafely(Path path) {
    return path.isAbsolute();
  }

  @Factory
  static Matcher<Path> isAbsolute() {
    return INSTANCE;
  }

  @Factory
  static Matcher<Path> isRelative() {
    return Matchers.not(INSTANCE);
  }

}

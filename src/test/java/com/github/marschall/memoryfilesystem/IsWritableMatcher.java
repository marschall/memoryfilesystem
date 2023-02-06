package com.github.marschall.memoryfilesystem;

import java.nio.file.Files;
import java.nio.file.Path;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

final class IsWritableMatcher extends TypeSafeMatcher<Path> {

  private static final TypeSafeMatcher<Path> INSTANCE = new IsWritableMatcher();

  @Override
  public void describeTo(Description description) {
     description.appendText("is writable");
  }

  @Override
  protected boolean matchesSafely(Path item) {
    return Files.isWritable(item);
  }

  static Matcher<Path> isWritable() {
    return INSTANCE;
  }
}


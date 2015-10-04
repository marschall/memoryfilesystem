package com.github.marschall.memoryfilesystem;

import java.nio.file.Files;
import java.nio.file.Path;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

final class IsSymbolicLinkMatcher extends TypeSafeMatcher<Path> {

  static Matcher<Path> isSymbolicLink() {
    return new IsSymbolicLinkMatcher();
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("file is a symbolic link");
  }

  @Override
  protected boolean matchesSafely(Path path) {
    return Files.isSymbolicLink(path);
  }

}

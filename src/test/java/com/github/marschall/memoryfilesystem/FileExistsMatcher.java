package com.github.marschall.memoryfilesystem;

import java.nio.file.Files;
import java.nio.file.Path;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

final class FileExistsMatcher extends TypeSafeMatcher<Path> {

  @Factory
  static Matcher<Path> fileExists() {
    return new FileExistsMatcher();
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("file exists");
  }

  @Override
  protected boolean matchesSafely(Path path) {
    return Files.exists(path);
  }

}

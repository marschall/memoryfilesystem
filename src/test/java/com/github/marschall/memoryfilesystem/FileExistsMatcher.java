package com.github.marschall.memoryfilesystem;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

final class FileExistsMatcher extends TypeSafeMatcher<Path> {

  private static final LinkOption[] NO_OPTIONS = new LinkOption[0];

  private final LinkOption[] options;

  private FileExistsMatcher(LinkOption[] options) {
    this.options = options;
  }

  @Factory
  static Matcher<Path> exists() {
    return new FileExistsMatcher(NO_OPTIONS);
  }

  @Factory
  static Matcher<Path> exists(LinkOption... options) {
    return new FileExistsMatcher(options);
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("file exists");
  }

  @Override
  protected boolean matchesSafely(Path path) {
    return Files.exists(path, this.options);
  }

}

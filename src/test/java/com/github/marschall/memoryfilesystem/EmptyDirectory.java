package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

final class EmptyDirectory extends TypeSafeMatcher<Path> {

  private static final TypeSafeMatcher<Path> INSTANCE = new EmptyDirectory();

  @Override
  public void describeTo(Description description) {
    description.appendText("is empty");
  }

  @Override
  protected boolean matchesSafely(Path path) {
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
      return !directoryStream.iterator().hasNext();
    } catch (IOException e) {
      throw new RuntimeException("could not check directory: " + path, e);
    }
  }

  static Matcher<Path> emptyDirectory() {
    return INSTANCE;
  }

}


package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

final class DirectoryHasEntry extends TypeSafeMatcher<Path> {


  private final String entryName;

  private DirectoryHasEntry(String entryName) {
    this.entryName = entryName;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("has entry ");
    description.appendValue(this.entryName);
  }

  @Override
  protected boolean matchesSafely(Path item) {
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(item)) {
      for (Path each : stream) {
        if (this.entryName.equals(each.getFileName().toString())) {
          return true;
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return false;
  }

  static Matcher<Path> hasEntry(String entryName) {
    return new DirectoryHasEntry(entryName);
  }

}

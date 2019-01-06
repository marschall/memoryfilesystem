package com.github.marschall.memoryfilesystem;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.file.StandardOpenOption.READ;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

class FileContentsMatcher extends TypeSafeMatcher<Path> {

  private final String contents;

  private FileContentsMatcher(String contents) {
    this.contents = contents;
  }

  static Matcher<Path> hasContents(String contents) {
    return new FileContentsMatcher(contents);
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("has contents ");
    description.appendValue(this.contents);
  }

  @Override
  protected boolean matchesSafely(Path path) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(this.contents.length());
    try {
      try (InputStream input = Files.newInputStream(path, READ)) {
        int read;
        byte[] buffer = new byte[512];
        while ((read = input.read(buffer)) != -1) {
          outputStream.write(buffer, 0, read);
        }
      }
      return this.contents.equals(new String(outputStream.toByteArray(), US_ASCII));
    } catch (IOException e) {
      return false;
    }
  }

}

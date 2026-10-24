package com.github.marschall.memoryfilesystem;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.assertj.core.api.AbstractAssert;

final class AdditionalPathAssert extends AbstractAssert<AdditionalPathAssert, Path> {

  AdditionalPathAssert(Path actual) {
    super(actual, AdditionalPathAssert.class);
  }

  static AdditionalPathAssert assertThat(Path actual) {
    return new AdditionalPathAssert(actual);
  }

  AdditionalPathAssert isSameFileAs(Path other) {
    this.isNotNull();
    assertNotNull(this.actual);

    try {
      if (!Files.isSameFile(this.actual, other)) {
        this.failWithMessage("Expected <%s> to be the same file as <%s> but was not", this.actual, other);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(String.format("Unable to verify files are the same:<%s> <%s>", this.actual, other), e);
    }

    return this;
  }

  AdditionalPathAssert isNotSameFileAs(Path other) {
    this.isNotNull();
    assertNotNull(this.actual);

    try {
      if (Files.isSameFile(this.actual, other)) {
        this.failWithMessage("Expected <%s> to not be the same file as <%s> but was", this.actual, other);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(String.format("Unable to verify files are the same:<%s> <%s>", this.actual, other), e);
    }

    return this;
  }

  AdditionalPathAssert isNotReadable() {
    this.isNotNull();

    if (Files.isReadable(this.actual)) {
      this.failWithMessage("Expected <%s> to not be readable but was", this.actual);
    }

    return this;
  }

  AdditionalPathAssert isNotWritable() {
    this.isNotNull();

    if (Files.isWritable(this.actual)) {
      this.failWithMessage("Expected <%s> to not be writable but was", this.actual);
    }

    return this;
  }

  AdditionalPathAssert isNotExecutable() {
    this.isNotNull();

    if (Files.isExecutable(this.actual)) {
      this.failWithMessage("Expected <%s> to not be writable but was", this.actual);
    }

    return this;
  }


  AdditionalPathAssert isHidden() {
    this.isNotNull();

    try {
      if (!Files.isHidden(this.actual)) {
        this.failWithMessage("Expected <%s> to be hidden but was not", this.actual);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(String.format("Unable to check:<%s>", this.actual), e);
    }

    return this;
  }

  AdditionalPathAssert isNotHidden() {
    this.isNotNull();

    try {
      if (Files.isHidden(this.actual)) {
        this.failWithMessage("Expected <%s> to not be writable but was", this.actual);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(String.format("Unable to check:<%s>", this.actual), e);
    }

    return this;
  }

  // TODO has content optimized

  AdditionalPathAssert hasEntryNamed(String fileName) {
    this.isNotNull();

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.actual)) {
      for (Path each : stream) {
        if (fileName.equals(each.getFileName().toString())) {
          return this;
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(String.format("Unable to iterate over:<%s>", this.actual), e);
    }

    this.failWithMessage("Expected to find entry named <%s> in <%s> to not be writable but was", fileName, this.actual);
    return this;
  }

  AdditionalPathAssert doesNotHaveEntryNamed(String fileName) {
    this.isNotNull();

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.actual)) {
      for (Path each : stream) {
        if (fileName.equals(each.getFileName().toString())) {
          this.failWithMessage("Did not expecte to find entry named <%s> in <%s> but did", fileName, this.actual);
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(String.format("Unable to iterate over:<%s>", this.actual), e);
    }

    return this;
  }

}

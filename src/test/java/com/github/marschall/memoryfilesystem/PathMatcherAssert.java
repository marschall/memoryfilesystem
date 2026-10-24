package com.github.marschall.memoryfilesystem;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.nio.file.PathMatcher;

import org.assertj.core.api.AbstractAssert;

final class PathMatcherAssert extends AbstractAssert<PathMatcherAssert, PathMatcher> {

  PathMatcherAssert(PathMatcher actual) {
    super(actual, PathMatcherAssert.class);
  }

  static PathMatcherAssert assertThat(PathMatcher actual) {
    return new PathMatcherAssert(actual);
  }

  PathMatcherAssert matches(Path other) {
    this.isNotNull();
    assertNotNull(this.actual);

    if (!this.actual.matches(other)) {
      this.failWithMessage("Expected <%s> to match <%s> but did not", this.actual, other);
    }

    return this;
  }

  PathMatcherAssert doesNotMatch(Path other) {
    this.isNotNull();
    assertNotNull(this.actual);

    if (this.actual.matches(other)) {
      this.failWithMessage("Expected <%s> not to match <%s> but did", this.actual, other);
    }

    return this;
  }

}

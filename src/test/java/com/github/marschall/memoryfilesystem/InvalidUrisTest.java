package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.Constants.SAMPLE_ENV;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class InvalidUrisTest {

  private static final String DISPLAY_NAME = "URI: {0}";

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("data")
  void invalidUri(URI uri) {
    assertThrows(IllegalArgumentException.class, () -> FileSystems.newFileSystem(uri, SAMPLE_ENV), uri + " should not be a valid URI");
  }

  static List<Object[]> data() {
    return Arrays.asList(new Object[][] {
            { URI.create("memory:name#fragment") },
            { URI.create("memory://user:pass@host:666/path?query#fragmet") },
            { URI.create("memory://host") },
            { URI.create("memory:///path") },
            { URI.create("memory:name//user:pass@host:666/path?query#fragmet") },
    });
  }

}

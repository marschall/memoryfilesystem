package com.google.code.memoryfilesystem;

import static com.google.code.memoryfilesystem.Constants.SAMPLE_ENV;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class InvalidUrisTest {

  private final URI uri;

  public InvalidUrisTest(URI env) {
    this.uri = env;
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidUri() throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(uri, SAMPLE_ENV)) {
      fail(this.uri + " should not be a valid URI");
    }
  }

  @Parameters
  public static List<Object[]> data() {
    return Arrays.asList(new Object[][] {
        { URI.create("memory:name#fragment") },
        { URI.create("memory://user:pass@host:666/path?query#fragmet") },
        { URI.create("memory://host") },
        { URI.create("memory:///path") },
        { URI.create("memory:name//user:pass@host:666/path?query#fragmet") }
    });
  }

}

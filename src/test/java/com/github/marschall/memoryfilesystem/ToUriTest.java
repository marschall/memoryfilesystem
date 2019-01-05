package com.github.marschall.memoryfilesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ToUriTest {

  private static final String DISPLAY_NAME = "path: {0}";

  @RegisterExtension
  public final FileSystemExtension rule = new FileSystemExtension();

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("data")
  public void contract(String path) {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path p = fileSystem.getPath(path);
    assertEquals(p.toAbsolutePath(), Paths.get(p.toUri()));
  }

  public static List<Object[]> data() {
    return Arrays.asList(new Object[][] {
            { "a" },
            { "/a" },
            { "a/b" },
            { "/a/b" },
            { "" },
            { ".." },
            { "." },
            { "/" },
    });
  }

}

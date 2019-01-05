package com.github.marschall.memoryfilesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests the contract outlined in {@link Path#relativize(Path)}.
 */
public class RelativizeResolveTest {

  private static final String DISPLAY_NAME = "receiver: {0}, other: {1}";

  @RegisterExtension
  public final FileSystemExtension rule = new FileSystemExtension();

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("data")
  public void contract(String first, String second) {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path p = fileSystem.getPath(first);
    Path q = fileSystem.getPath(second);
    assertEquals(q, p.relativize(p.resolve(q)));
  }

  public static List<Object[]> data() {
    return Arrays.asList(new Object[][] {
            { "a", "a" },
            { "/a", "a" },
            { "a", "a/b" },
            { "a/b", "a/b" },
            { "/a", "a/b" },
            { "/a/b", "a/b" },
            { "a", "c" },
            { "/a", "c" },
            { "a", "c/d" },
            { "a/b", "c/d" },
            { "/a", "c/d" },
            { "/a/b", "c/d" },
            { "/", "" },
            { "/", "a" },
            { "a", "" },
            { "/a", "" },
            { "", "" },
            { "", "a" },
    });
  }


}

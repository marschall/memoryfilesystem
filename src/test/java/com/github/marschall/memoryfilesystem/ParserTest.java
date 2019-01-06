package com.github.marschall.memoryfilesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.FileSystem;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ParserTest {

  private static final String DISPLAY_NAME = "first: {0}, more: {1}, expected: {2}";

  @RegisterExtension
  final FileSystemExtension rule = new FileSystemExtension();


  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("data")
  void parse(String first, String[] more, String expected) {
    FileSystem fileSystem = this.rule.getFileSystem();
    assertEquals(fileSystem.getPath(expected), fileSystem.getPath(first, more));
  }

  static List<Object[]> data() {
    return Arrays.asList(new Object[][] {
            { "", null, "" },
            { "a", null, "a" },
            { "/a", null, "/a" },
            { "/a", new String[]{"b", "c"}, "/a/b/c" },
            { "/a", new String[]{"/b/", "c"}, "/a/b/c" },
            { "/a", new String[]{"/b//", "c"}, "/a/b/c" },
            { "/a", new String[]{"//b//", "c"}, "/a/b/c" },
            { "/a", new String[]{"//b/", "c"}, "/a/b/c" },
            { "/a/", new String[]{"//b/", "c"}, "/a/b/c" },
            { "//a/", new String[]{"//b/", "c"}, "/a/b/c" },
            { "//a//", new String[]{"//b/", "c"}, "/a/b/c" },
            { "/a//", new String[]{"//b/", "c"}, "/a/b/c" },
            { "/a//", new String[]{"//b/c"}, "/a/b/c" },
            { "/a//", new String[]{"//b//c"}, "/a/b/c" },
            { "/a//", new String[]{"//b//c/"}, "/a/b/c" },
            { "/a//", new String[]{"//b//c//"}, "/a/b/c" },
            { "/", new String[]{"a", "b", "c"}, "/a/b/c" },
            { "//", new String[]{"/a", "/b", "/c"}, "/a/b/c" },
            { "/", new String[]{"/a", "/b", "/c"}, "/a/b/c" },
            { "/", new String[]{"a/", "b/", "c/"}, "/a/b/c" },
            { "", new String[]{"/a/", "b/", "c/"}, "/a/b/c" },
            { "", new String[]{"", "", "/a/b/c"}, "/a/b/c" },
            { "", new String[]{"", "", "/a/b/c", ""}, "/a/b/c" },
            { "", new String[]{"", "/a/", "b/c", ""}, "/a/b/c" },
            { "/a/b/c", new String[0], "/a/b/c" },
    });
  }

}

package com.github.marschall.memoryfilesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ParameterizedSingleEmptyRootPathParserTest {

  private static final String DISPLAY_NAME = "first: {0} more: {1}";

  @RegisterExtension
  final FileSystemExtension rule = new FileSystemExtension();

  private Path expected;
  private PathParser parser;

  static List<Object[]> data() {
    return Arrays.asList(new Object[][] {
            { "/a", new String[]{"b", "c"} },
            { "/a", new String[]{"/b/", "c"} },
            { "/a", new String[]{"/b//", "c"} },
            { "/a", new String[]{"//b//", "c"} },
            { "/a", new String[]{"//b/", "c"} },
            { "/a/", new String[]{"//b/", "c"} },
            { "//a/", new String[]{"//b/", "c"} },
            { "//a//", new String[]{"//b/", "c"} },
            { "/a//", new String[]{"//b/", "c"} },
            { "/a//", new String[]{"//b/c"} },
            { "/a//", new String[]{"//b//c"} },
            { "/a//", new String[]{"//b//c/"} },
            { "/a//", new String[]{"//b//c//"} },
            { "/", new String[]{"a", "b", "c"} },
            { "//", new String[]{"/a", "/b", "/c"} },
            { "/", new String[]{"/a", "/b", "/c"} },
            { "/", new String[]{"a/", "b/", "c/"} },
            { "", new String[]{"/a/", "b/", "c/"} },
            { "", new String[]{"", "", "/a/b/c"} },
            { "", new String[]{"", "", "/a/b/c", ""} },
            { "", new String[]{"", "/a/", "b/c", ""} },
            { "/a/b/c", new String[0] },
    });
  }

  @BeforeEach
  void setUp() {
    this.parser = new SingleEmptyRootPathParser("/", EmptyCharacterSet.INSTANCE);
    Root root = (Root) this.rule.getFileSystem().getRootDirectories().iterator().next();
    this.expected = AbstractPath.createAbsolute(root.getMemoryFileSystem(), root, Arrays.asList("a", "b", "c"));
  }

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("data")
  void test(String first, String[] more) {
    Root root = (Root) this.rule.getFileSystem().getRootDirectories().iterator().next();
    Path actual = this.parser.parse(Collections.singletonMap("", root), first, more);
    assertEquals(this.expected, actual);
  }

}

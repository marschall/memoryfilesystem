package com.github.marschall.memoryfilesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class RegexPathMatcherTest {

  @RegisterExtension
  final FileSystemExtension rule = new FileSystemExtension();

  @ParameterizedTest
  @MethodSource("data")
  void matchesUpperCase(String pattern, String p, boolean expected) {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path path = fileSystem.getPath(p);
    PathMatcher matcher = fileSystem.getPathMatcher(RegexPathMatcher.name().toUpperCase() + ":" + pattern);
    assertEquals(expected, matcher.matches(path));
  }

  @ParameterizedTest
  @MethodSource("data")
  void matchesLowerCase(String pattern, String p, boolean expected) {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path path = fileSystem.getPath(p);
    PathMatcher matcher = fileSystem.getPathMatcher(RegexPathMatcher.name().toLowerCase() + ":" + pattern);
    assertEquals(expected, matcher.matches(path));
  }

  static List<Object[]> data() {
    return Arrays.asList(new Object[][] {
            { ".*\\.java", "GlobPathMatcherTest.java", true },
            { ".*\\.java", ".java", true },
            { ".*\\.java", ".jav.java", true },
            { ".*\\.java", ".jav.java", true },
            { ".*\\.java", ".jav", false },
            { ".*\\.java", "java", false },

            { ".*\\..*", "GlobPathMatcherTest.java", true },
            { ".*\\..*", ".java", true },
            { ".*\\..*", ".jav.java", true },
            { ".*\\..*", ".jav.java", true },
            { ".*\\..*", "jav.java", true },
            { ".*\\..*", "java.", true },
            { ".*\\..*", "java", false },

            { ".*\\.((java)|(class))", "GlobPathMatcherTest.java", true },
            { ".*\\.((java)|(class))", ".java", true },
            { ".*\\.((java)|(class))", ".jav.java", true },
            { ".*\\.((java)|(class))", ".jav.java", true },
            { ".*\\.((java)|(class))", ".jav", false },
            { ".*\\.((java)|(class))", "java", false },
            { ".*\\.((java)|(class))", "GlobPathMatcherTest.class", true },
            { ".*\\.((java)|(class))", ".class", true },
            { ".*\\.((java)|(class))", ".clas.class", true },
            { ".*\\.((java)|(class))", ".clas.class", true },
            { ".*\\.((java)|(class))", ".clas", false },
            { ".*\\.((java)|(class))", "class", false },

            { "foo\\..", "foo.", false },
            { "foo\\..", "foo.?.", false },
            { "foo\\..", "foo.?", true },
            { "foo\\..", "foo.f", true },

            { "/home/[^/]*/[^/]*", "/home/gus/data", true },
            { "/home/[^/]*/[^/]*", "/home/gus/.data", true },
            { "/home/[^/]*/[^/]*", "/home/gus/data/backup", false },
            { "/home/[^/]*/[^/]*", "/home/gus", false },

            { "/home/.*", "/home/gus/data", true },
            { "/home/.*", "/home/gus", true },

            //        { "C:\\\\*", "C:\\foo", true },
            //        { "C:\\\\*", "C:\\bar", true },

    });
  }

}

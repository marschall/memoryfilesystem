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


public class GlobPathMatcherTest {

  private static final String DISPLAY_NAME = "pattern: {0}, path: {1}, should match: {2}";

  @RegisterExtension
  public final FileSystemExtension rule = new FileSystemExtension();

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("data")
  public void matchesUpperCase(String pattern, String p, boolean expected) {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path path = fileSystem.getPath(p);
    PathMatcher matcher = fileSystem.getPathMatcher(GlobPathMatcher.name().toUpperCase() + ":" + pattern);
    assertEquals(expected, matcher.matches(path));
  }

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("data")
  public void lowerUpperCase(String pattern, String p, boolean expected) {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path path = fileSystem.getPath(p);
    PathMatcher matcher = fileSystem.getPathMatcher(GlobPathMatcher.name().toLowerCase() + ":" + pattern);
    assertEquals(expected, matcher.matches(path));
  }

  public static List<Object[]> data() {
    return Arrays.asList(new Object[][] {
      { "*.java", "GlobPathMatcherTest.java", true },
      { "*.java", "GlobPathMatcherTest.JAVA", false },
      { "*.JAVA", "GlobPathMatcherTest.java", false },
      { "*.j[a-z]va", "GlobPathMatcherTest.java", true },
      { "*.j[A-Z]va", "GlobPathMatcherTest.java", false },
      { "*.java", ".java", true },
      { "*.java", ".jav.java", true },
      { "*.java", ".jav.java", true },
      { "*.java", ".jav", false },
      { "*.java", "java", false },

      { "*.*", "GlobPathMatcherTest.java", true },
      { "*.*", ".java", true },
      { "*.*", ".jav.java", true },
      { "*.*", ".jav.java", true },
      { "*.*", "jav.java", true },
      { "*.*", "java.", true },
      { "*.*", "java", false },

      { "*.{java,class}", "GlobPathMatcherTest.java", true },
      { "*.{java,class}", ".java", true },
      { "*.{java,class}", ".jav.java", true },
      { "*.{java,class}", ".jav.java", true },
      { "*.{java,class}", ".jav", false },
      { "*.{java,class}", "java", false },
      { "*.{java,class}", "GlobPathMatcherTest.class", true },
      { "*.{java,class}", ".class", true },
      { "*.{java,class}", ".clas.class", true },
      { "*.{java,class}", ".clas.class", true },
      { "*.{java,class}", ".clas", false },
      { "*.{java,class}", "class", false },

      { "foo.?", "foo.", false },
      { "foo.?", "foo.?.", false },
      { "foo.?", "foo.?", true },
      { "foo.?", "foo.f", true },

      { "/home/*/*", "/home/gus/data", true },
      { "/home/*/*", "/home/gus/.data", true },
      { "/home/*/*", "/home/gus/data/backup", false },
      { "/home/*/*", "/home/gus", false },

      { "/home/**", "/home/gus/data", true },
      { "/home/**", "/home/gus", true },

      { "/home/**/a/b/**/c", "/home/x/a/b/x/c", true },
      { "/home/**/a/b/**/c", "/home/x/a/x/a/b/x/c", true },
      { "/home/**/a/b/**/c", "/home/a/b/c", false }

    });
  }

}

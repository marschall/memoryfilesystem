package com.github.marschall.memoryfilesystem;

import static org.junit.Assert.assertEquals;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


@RunWith(Parameterized.class)
@Ignore("not yet implemented")
public class GlobPathMatcherTest {

  @Rule
  public final FileSystemRule rule = new FileSystemRule();

  private final String p;
  private final boolean expected;
  private final String pattern;

  public GlobPathMatcherTest(String path, String pattern, boolean expected) {
    this.p = path;
    this.expected = expected;
    this.pattern = pattern;
  }

  @Test
  public void matches() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path path = fileSystem.getPath(this.p);
    PathMatcher matcher = fileSystem.getPathMatcher(GlobPathMatcher.name() + ":" + this.pattern);
    assertEquals(this.expected, matcher.matches(path));
  }

  @Parameters
  public static List<Object[]> data() {
    return Arrays.asList(new Object[][] {
            { "*.java", "GlobPathMatcherTest.java", true },
            { "*.java", "GlobPathMatcherTest.JAVA", true },
            { "*.JAVA", "GlobPathMatcherTest.java", true },
            { "*.j[a-z]va", "GlobPathMatcherTest.java", true },
            { "*.j[A-Z]va", "GlobPathMatcherTest.java", true },
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

            { "C:\\\\*", "C:\\foo", true },
            { "C:\\\\*", "C:\\bar", true },

    });
  }

}

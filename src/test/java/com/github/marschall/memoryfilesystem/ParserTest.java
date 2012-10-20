package com.github.marschall.memoryfilesystem;

import static org.junit.Assert.assertEquals;

import java.nio.file.FileSystem;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ParserTest {

  @Rule
  public final FileSystemRule rule = new FileSystemRule();

  private final String first;
  private final String[] more;

  private final String expected;


  public ParserTest(String first, String[] more, String expected) {
    this.first = first;
    this.more = more;
    this.expected = expected;
  }

  @Test
  public void parse() {
    FileSystem fileSystem = this.rule.getFileSystem();
    assertEquals(fileSystem.getPath(this.expected), fileSystem.getPath(this.first, this.more));
  }

  @Parameters
  public static List<Object[]> data() {
    return Arrays.asList(new Object[][] {
            { "", null, "" },
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

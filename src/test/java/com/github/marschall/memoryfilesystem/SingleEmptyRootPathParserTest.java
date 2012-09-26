package com.github.marschall.memoryfilesystem;

import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SingleEmptyRootPathParserTest {

  @Rule
  public final FileSystemRule rule = new FileSystemRule();
  
  private final String first;
  private final String[] more;

  private Path expected;

  private PathParser parser;
  
  
  public SingleEmptyRootPathParserTest(String first, String[] more) {
    this.first = first;
    this.more = more;
  }
  
  @Parameters
  public static List<Object[]> data() {
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
        { "/a/b/c", new String[0] }
    });
  }

  @Before
  public void setUp() {
    parser = new SingleEmptyRootPathParser();
    Root root = (Root) rule.getFileSystem().getRootDirectories().iterator().next();
    expected = AbstractPath.createAboslute(root.getMemoryFileSystem(), root, Arrays.asList("a", "b", "c"));
  }
  
  @Test
  public void test() {
//    Path actual = rule.getFileSystem().getPath(first, more);
    Root root = (Root) rule.getFileSystem().getRootDirectories().iterator().next();
    Path actual = this.parser.parse(Collections.singletonList(root), first, more);
    assertEquals(expected, actual);
  }

}

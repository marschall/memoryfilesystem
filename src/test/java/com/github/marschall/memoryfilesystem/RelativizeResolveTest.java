package com.github.marschall.memoryfilesystem;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests the contract outlined in {@link Path#relativize(Path)}. 
 */
@RunWith(Parameterized.class)
public class RelativizeResolveTest {

  @Rule
  public final FileSystemRule rule = new FileSystemRule();

  private final String first;
  private final String second;

  public RelativizeResolveTest(String first, String second) {
    this.first = first;
    this.second = second;
  }
  
  @Test
  public void contract() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path p = fileSystem.getPath(this.first);
    Path q = fileSystem.getPath(this.second);
    assertEquals(q, p.relativize(p.resolve(q)));
  }
  
  @Parameters
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

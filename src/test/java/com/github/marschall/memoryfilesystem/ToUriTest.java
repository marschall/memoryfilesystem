package com.github.marschall.memoryfilesystem;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ToUriTest {
  
  @Rule
  public final FileSystemRule rule = new FileSystemRule();

  private final String path;

  public ToUriTest(String path) {
    this.path = path;
  }
  
  @Test
  public void contract() throws IOException {
    FileSystem fileSystem = rule.getFileSystem();
    Path p = fileSystem.getPath(this.path);
    assertEquals(p.toAbsolutePath(), Paths.get(p.toUri()));
  }
  
  @Parameters
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

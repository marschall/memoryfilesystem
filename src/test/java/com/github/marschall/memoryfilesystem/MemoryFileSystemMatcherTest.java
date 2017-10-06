package com.github.marschall.memoryfilesystem;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.regex.PatternSyntaxException;

import org.junit.Rule;
import org.junit.Test;

public class MemoryFileSystemMatcherTest {

  @Rule
  public final FileSystemRule rule = new FileSystemRule();

  @Test(expected = UnsupportedOperationException.class)
  public void getPathMatcherUnknown() {
    FileSystem fileSystem = this.rule.getFileSystem();
    fileSystem.getPathMatcher("syntax:patten");
  }

  @Test(expected = IllegalArgumentException.class)
  public void getPathMatcherInvalid1() {
    FileSystem fileSystem = this.rule.getFileSystem();
    fileSystem.getPathMatcher("invalid");
  }

  @Test(expected = IllegalArgumentException.class)
  public void getPathMatcherInvalid2() {
    FileSystem fileSystem = this.rule.getFileSystem();
    fileSystem.getPathMatcher("invalid:");
  }

  @Test
  public void getPathMatcherGlob() {
    FileSystem fileSystem = this.rule.getFileSystem();
    PathMatcher matcher = fileSystem.getPathMatcher("glob:*.java");
    assertTrue(matcher instanceof GlobPathMatcher);
  }

  @Test
  public void getPathMatcherRegex() {
    FileSystem fileSystem = this.rule.getFileSystem();
    PathMatcher matcher = fileSystem.getPathMatcher("regex:.*\\.java");
    assertTrue(matcher instanceof RegexPathMatcher);
  }

  @Test(expected = PatternSyntaxException.class)
  public void getPathMatcherRegexInvalid() {
    FileSystem fileSystem = this.rule.getFileSystem();
    PathMatcher matcher = fileSystem.getPathMatcher("regex:*\\.java");
    assertTrue(matcher instanceof RegexPathMatcher);
  }

  @Test
  public void regression91() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

    Path parent = fileSystem.getPath("/a/b");
    Files.createDirectories(parent);

    Path child = parent.resolve(".gitignore");
    Files.createFile(child);

    PathMatcher matcher = fileSystem.getPathMatcher("glob:**/.gitignore");
    assertTrue(matcher.matches(child));
  }

}

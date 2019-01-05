package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.PathMatchesMatcher.matches;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.regex.PatternSyntaxException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class MemoryFileSystemMatcherTest {

  @RegisterExtension
  public final FileSystemExtension rule = new FileSystemExtension();

  @Test
  public void getPathMatcherUnknown() {
    FileSystem fileSystem = this.rule.getFileSystem();
    assertThrows(UnsupportedOperationException.class, () -> fileSystem.getPathMatcher("syntax:patten"));
  }

  @Test
  public void getPathMatcherInvalid1() {
    FileSystem fileSystem = this.rule.getFileSystem();
    assertThrows(IllegalArgumentException.class, () -> fileSystem.getPathMatcher("invalid"));
  }

  @Test
  public void getPathMatcherInvalid2() {
    FileSystem fileSystem = this.rule.getFileSystem();
    assertThrows(IllegalArgumentException.class, () -> fileSystem.getPathMatcher("invalid:"));
  }

  @Test
  @Disabled
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

  @Test
  public void getPathMatcherRegexInvalid() {
    FileSystem fileSystem = this.rule.getFileSystem();
    assertThrows(PatternSyntaxException.class, () -> fileSystem.getPathMatcher("regex:*\\.java"));
  }

  @Test
  public void regression91() {
    FileSystem fileSystem = this.rule.getFileSystem();

    PathMatcher matcher = fileSystem.getPathMatcher("glob:{,**/}.gitignore");
    assertThat(matcher, matches(fileSystem.getPath(".gitignore")));
    assertThat(matcher, matches(fileSystem.getPath("src/.gitignore")));
  }

  @Test
  public void regression92() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

    Path parent = fileSystem.getPath("/a/b");
    Files.createDirectories(parent);

    Path child = parent.resolve(".gitignore");
    Files.createFile(child);

    PathMatcher matcher = fileSystem.getPathMatcher("glob:**/.gitignore");
    assertThat(matcher, matches(child));
  }

}

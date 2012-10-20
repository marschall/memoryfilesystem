package com.github.marschall.memoryfilesystem;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class EnvironmentParserTest {

  @Test
  public void empty() {
    EnvironmentParser parser = this.parse(MemoryFileSystemBuilder.newEmpty());
    assertEquals(Collections.singletonList("/"), parser.getRoots());
    assertEquals("/", parser.getSeparator());
  }

  @Test
  public void unix() {
    EnvironmentParser parser = this.parse(MemoryFileSystemBuilder.newUnix());
    assertEquals(Collections.singletonList("/"), parser.getRoots());
    assertEquals("/", parser.getSeparator());
  }

  @Test
  public void windows() {
    EnvironmentParser parser = this.parse(MemoryFileSystemBuilder.newWindows());
    assertEquals(Collections.singletonList("C:\\"), parser.getRoots());
    assertEquals("\\", parser.getSeparator());
  }

  @Test
  public void windowsExtended() {
    EnvironmentParser parser = this.parse(MemoryFileSystemBuilder.newWindows().addRoot("D:\\"));
    assertEquals(Arrays.asList("C:\\", "D:\\"), parser.getRoots());
    assertEquals("\\", parser.getSeparator());
  }

  private EnvironmentParser parse(MemoryFileSystemBuilder builder) {
    return new EnvironmentParser(builder.buildEnvironment());
  }

}

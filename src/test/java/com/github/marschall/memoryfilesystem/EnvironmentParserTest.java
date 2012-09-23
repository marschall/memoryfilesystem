package com.github.marschall.memoryfilesystem;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import com.github.marschall.memoryfilesystem.EnvironmentBuilder;
import com.github.marschall.memoryfilesystem.EnvironmentParser;

public class EnvironmentParserTest {

  @Test
  public void empty() {
    EnvironmentParser parser = parse(EnvironmentBuilder.newEmpty());
    assertEquals(Collections.singletonList(""), parser.getRoots());
    assertEquals("/", parser.getSeparator());
  }

  @Test
  public void unix() {
    EnvironmentParser parser = parse(EnvironmentBuilder.newUnix());
    assertEquals(Collections.singletonList(""), parser.getRoots());
    assertEquals("/", parser.getSeparator());
  }

  @Test
  public void windows() {
    EnvironmentParser parser = parse(EnvironmentBuilder.newWindows());
    assertEquals(Collections.singletonList("C:\\"), parser.getRoots());
    assertEquals("\\", parser.getSeparator());
  }

  @Test
  public void windowsExtended() {
    EnvironmentParser parser = parse(EnvironmentBuilder.newWindows().addRoot("D:\\"));
    assertEquals(Arrays.asList("C:\\", "D:\\"), parser.getRoots());
    assertEquals("\\", parser.getSeparator());
  }

  private EnvironmentParser parse(EnvironmentBuilder builder) {
    return new EnvironmentParser(builder.build());
  }

}

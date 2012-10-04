package com.github.marschall.memoryfilesystem;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SingleEmptyRootPathParserTest {

  @Test
  public void count() {
    assertEquals(0, SingleEmptyRootPathParser.count(""));
    assertEquals(1, SingleEmptyRootPathParser.count("a"));
    assertEquals(2, SingleEmptyRootPathParser.count("a/a"));
    assertEquals(2, SingleEmptyRootPathParser.count("a/a/"));
    assertEquals(2, SingleEmptyRootPathParser.count("/a/a"));
    assertEquals(2, SingleEmptyRootPathParser.count("/a/a/"));
    assertEquals(2, SingleEmptyRootPathParser.count("/a//a/"));
    assertEquals(2, SingleEmptyRootPathParser.count("/a/a//"));
    assertEquals(2, SingleEmptyRootPathParser.count("//a//a/"));
  }

}

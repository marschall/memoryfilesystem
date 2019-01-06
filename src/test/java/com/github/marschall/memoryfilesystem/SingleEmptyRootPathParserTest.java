package com.github.marschall.memoryfilesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SingleEmptyRootPathParserTest {

  @Test
  void count() {
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

package com.github.marschall.memoryfilesystem;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmptyCharacterSetTest {

  private CharacterSet characterSet;

  @BeforeEach
  void setUp() {
    this.characterSet = EmptyCharacterSet.INSTANCE;
  }

  @Test
  void test() {
    assertFalse(this.characterSet.containsAny(""));
    assertFalse(this.characterSet.containsAny("a"));
    assertFalse(this.characterSet.containsAny("ab"));
  }

}

package com.github.marschall.memoryfilesystem;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EmptyCharacterSetTest {

  private CharacterSet characterSet;

  @BeforeEach
  public void setUp() {
    this.characterSet = EmptyCharacterSet.INSTANCE;
  }

  @Test
  public void test() {
    assertFalse(this.characterSet.containsAny(""));
    assertFalse(this.characterSet.containsAny("a"));
    assertFalse(this.characterSet.containsAny("ab"));
  }

}

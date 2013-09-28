package com.github.marschall.memoryfilesystem;

import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;

public class EmptyCharacterSetTest {

  private CharacterSet characterSet;

  @Before
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

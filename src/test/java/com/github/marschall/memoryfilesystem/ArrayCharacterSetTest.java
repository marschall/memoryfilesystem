package com.github.marschall.memoryfilesystem;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArrayCharacterSetTest {

  private CharacterSet characterSet;

  @BeforeEach
  void setUp() {
    this.characterSet = new ArrayCharacterSet(new char[]{'a', 'b'});
  }

  @Test
  void containsChar() {
    assertTrue(this.characterSet.contains('a'));
    assertTrue(this.characterSet.contains('b'));
  }

  @Test
  void containsCharNot() {
    assertFalse(this.characterSet.contains('c'));
  }

  @Test
  void contains() {
    assertTrue(this.characterSet.containsAny("acc"));
    assertTrue(this.characterSet.containsAny("cac"));
    assertTrue(this.characterSet.containsAny("cca"));

    assertTrue(this.characterSet.containsAny("bcc"));
    assertTrue(this.characterSet.containsAny("cbc"));
    assertTrue(this.characterSet.containsAny("ccb"));
  }

  @Test
  void containsNot() {
    assertFalse(this.characterSet.containsAny(""));
    assertFalse(this.characterSet.containsAny("c"));
    assertFalse(this.characterSet.containsAny("cc"));
  }

}

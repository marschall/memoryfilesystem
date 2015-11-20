package com.github.marschall.memoryfilesystem;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class ArrayCharacterSetTest {

  private CharacterSet characterSet;

  @Before
  public void setUp() {
    this.characterSet = new ArrayCharacterSet(new char[]{'a', 'b'});
  }

  @Test
  public void containsChar() {
    assertTrue(this.characterSet.contains('a'));
    assertTrue(this.characterSet.contains('b'));
  }

  @Test
  public void containsCharNot() {
    assertFalse(this.characterSet.contains('c'));
  }

  @Test
  public void contains() {
    assertTrue(this.characterSet.containsAny("acc"));
    assertTrue(this.characterSet.containsAny("cac"));
    assertTrue(this.characterSet.containsAny("cca"));

    assertTrue(this.characterSet.containsAny("bcc"));
    assertTrue(this.characterSet.containsAny("cbc"));
    assertTrue(this.characterSet.containsAny("ccb"));
  }

  @Test
  public void containsNot() {
    assertFalse(this.characterSet.containsAny(""));
    assertFalse(this.characterSet.containsAny("c"));
    assertFalse(this.characterSet.containsAny("cc"));
  }

}

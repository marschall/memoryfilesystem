package com.github.marschall.memoryfilesystem;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.DELETE_ON_CLOSE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.OpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

public class DefaultOpenOptionsTest {

  private Set<OpenOption> options;
  private Set<OpenOption> expected;

  @Before
  public void setUp() {
    this.options = DefaultOpenOptions.INSTANCE;
    this.expected = new HashSet<OpenOption>(Arrays.asList(CREATE, TRUNCATE_EXISTING, WRITE));
  }

  @Test
  public void contains() {
    assertTrue(this.options.contains(CREATE));
    assertTrue(this.options.contains(TRUNCATE_EXISTING));
    assertTrue(this.options.contains(WRITE));
    assertFalse(this.options.contains(DELETE_ON_CLOSE));
  }

  @Test
  public void iterator() {
    Set<OpenOption> actual = new HashSet<>(3);
    for (OpenOption each : this.options) {
      assertTrue(actual.add(each));
    }
    assertEquals(actual, this.expected);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void add() {
    this.options.add(DELETE_ON_CLOSE);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void addAll() {
    this.options.addAll(Collections.singleton(DELETE_ON_CLOSE));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void remove() {
    this.options.remove(DELETE_ON_CLOSE);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void removeAll() {
    this.options.removeAll(Collections.singleton(DELETE_ON_CLOSE));
  }

  @Test
  public void size() {
    assertEquals(3, this.options.size());
  }

  @Test
  public void equals() {
    assertEquals(this.options, this.expected);
  }

}

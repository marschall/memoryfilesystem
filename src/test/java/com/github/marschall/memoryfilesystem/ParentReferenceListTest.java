package com.github.marschall.memoryfilesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class ParentReferenceListTest {

  @Test
  public void contains() {
    assertTrue(ParentReferenceList.create(42).contains(".."));
    assertFalse(ParentReferenceList.create(42).contains("."));
  }

  @Test
  public void create() {
    assertSame(Collections.emptyList(), ParentReferenceList.create(0));
    assertEquals(Collections.singletonList(".."), ParentReferenceList.create(1));
    assertEquals(Arrays.asList("..", ".."), ParentReferenceList.create(2));
  }

  @Test
  public void subList() {
    List<String> list = ParentReferenceList.create(42);
    assertSame(Collections.emptyList(), list.subList(7, 7));
    assertEquals(Collections.singletonList(".."), list.subList(7, 8));
    assertEquals(Arrays.asList("..", ".."), list.subList(7, 9));
    assertSame(list, list.subList(0, 42));
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void indexUnderflow() {
    ParentReferenceList.create(42).get(-1);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void indexOverflow() {
    ParentReferenceList.create(42).get(42);
  }

}

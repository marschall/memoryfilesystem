package com.github.marschall.memoryfilesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class HomogenousListTest {

  @Test
  public void create() {
    assertSame(Collections.emptyList(), HomogenousList.create("..", 0));
    assertEquals(Collections.singletonList(".."), HomogenousList.create("..", 1));
    assertEquals(Arrays.asList("..", ".."), HomogenousList.create("..", 2));
  }
  
  @Test
  public void subList() {
    List<String> list = HomogenousList.create("..", 42);
    assertSame(Collections.emptyList(), list.subList(7, 7));
    assertEquals(Collections.singletonList(".."), list.subList(7, 8));
    assertEquals(Arrays.asList("..", ".."), list.subList(7, 9));
    assertSame(list, list.subList(0, 42));
  }
  
  @Test(expected = IndexOutOfBoundsException.class)
  public void indexUnderflow() {
    HomogenousList.create("..", 42).get(-1);
  }
  
  @Test(expected = IndexOutOfBoundsException.class)
  public void indexOverflow() {
    HomogenousList.create("..", 42).get(42);
  }

}

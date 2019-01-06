package com.github.marschall.memoryfilesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class ParentReferenceListTest {

  @Test
  void contains() {
    assertTrue(ParentReferenceList.create(42).contains(".."));
    assertFalse(ParentReferenceList.create(42).contains("."));
  }

  @Test
  void create() {
    assertSame(Collections.emptyList(), ParentReferenceList.create(0));
    assertEquals(Collections.singletonList(".."), ParentReferenceList.create(1));
    assertEquals(Arrays.asList("..", ".."), ParentReferenceList.create(2));
  }

  @Test
  void subList() {
    List<String> list = ParentReferenceList.create(42);
    assertSame(Collections.emptyList(), list.subList(7, 7));
    assertEquals(Collections.singletonList(".."), list.subList(7, 8));
    assertEquals(Arrays.asList("..", ".."), list.subList(7, 9));
    assertSame(list, list.subList(0, 42));
  }

  @Test
  void indexUnderflow() {
    assertThrows(IndexOutOfBoundsException.class, () -> ParentReferenceList.create(42).get(-1));
  }

  @Test
  void indexOverflow() {
    assertThrows(IndexOutOfBoundsException.class, () -> ParentReferenceList.create(42).get(42));
  }

}

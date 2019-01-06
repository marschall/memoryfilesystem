package com.github.marschall.memoryfilesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class CompositeListTest {

  @Test
  void create() {
    List<String> a = Collections.singletonList("a");
    List<String> b = Collections.singletonList("b");

    assertEquals(Arrays.asList("a", "b"), CompositeList.create(a, b));
  }

  @Test
  void createOneEmpty() {
    List<String> emptyList = Collections.emptyList();
    List<String> singletonList = Collections.singletonList("x");
    assertSame(singletonList, CompositeList.create(emptyList, singletonList));
    assertSame(singletonList, CompositeList.create(singletonList, emptyList));
  }

  @Test
  void subList() {
    List<String> first = Arrays.asList("1", "2", "3");
    List<String> second = Arrays.asList("4", "5", "6");
    List<String> composite = CompositeList.create(first, second);

    assertSame(first, composite.subList(0, 3));
    assertSame(second, composite.subList(3, 6));

    assertSame(composite, composite.subList(0, 6));
    assertSame(Collections.emptyList(), composite.subList(0, 0));

    assertEquals(Collections.singletonList("1"), composite.subList(0, 1));

    assertEquals(Arrays.asList("1", "2"), composite.subList(0, 2));
    assertEquals(Arrays.asList("1", "2", "3", "4"), composite.subList(0, 4));
    assertEquals(Arrays.asList("2", "3", "4"), composite.subList(1, 4));
    assertEquals(Arrays.asList("3", "4"), composite.subList(2, 4));
    assertEquals(Arrays.asList("3", "4", "5"), composite.subList(2, 5));
    assertEquals(Arrays.asList("5", "6"), composite.subList(4, 6));
  }

}

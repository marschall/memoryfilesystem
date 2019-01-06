package com.github.marschall.memoryfilesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.Test;

class StringTransformersTest {

  @Test
  void identity() {
    StringTransformer transformer = StringTransformers.IDENTIY;
    assertEquals("aA", transformer.transform("aA"));
  }

  @Test
  void caseInsensitive() {
    StringTransformer transformer = StringTransformers.caseInsensitive(Locale.US);
    assertEquals("AA", transformer.transform("aA"));
  }

}

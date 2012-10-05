package com.github.marschall.memoryfilesystem;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.Test;

public class StringTransformersTest {

  @Test
  public void identiy() {
    StringTransformer transformer = StringTransformers.IDENTIY;
    assertEquals("aA", transformer.transform("aA"));
  }

  @Test
  public void caseInsensitive() {
    StringTransformer transformer = StringTransformers.caseInsensitive(Locale.US);
    assertEquals("AA", transformer.transform("aA"));
  }

}

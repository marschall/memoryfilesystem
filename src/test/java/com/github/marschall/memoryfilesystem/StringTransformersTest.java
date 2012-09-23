package com.github.marschall.memoryfilesystem;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.Test;

import com.github.marschall.memoryfilesystem.StringTransformer;
import com.github.marschall.memoryfilesystem.StringTransformers;

public class StringTransformersTest {

  @Test
  public void identiy() {
    StringTransformer transformer = StringTransformers.IDENTIY;
    assertEquals("aA", transformer.tranform("aA"));
  }

  @Test
  public void caseInsensitive() {
    StringTransformer transformer = StringTransformers.caseInsensitive(Locale.US);
    assertEquals("aa", transformer.tranform("aA"));
  }

}

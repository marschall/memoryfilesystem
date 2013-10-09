package com.github.marschall.memoryfilesystem;

import static org.junit.Assert.assertFalse;

import java.text.Collator;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Locale;

import org.junit.Ignore;
import org.junit.Test;

public class CollatorTest {

  @Test
  @Ignore
  public void test() {
    Locale locale = Locale.getDefault();
    Collator collator = Collator.getInstance(locale);
    //    collator.setStrength(Collator.SECONDARY);
    collator.setDecomposition(Collator.NO_DECOMPOSITION);
    //    collator.setDecomposition(Collator.NO_DECOMPOSITION);
    collator.setStrength(Collator.SECONDARY);
    //    collator.setStrength(Collator.IDENTICAL);

    String a = "a";
    String A = "A";

    String aUmlaut = "\u00C4";
    String normalized = Normalizer.normalize(aUmlaut, Form.NFD);

    //    assertTrue(collator.equals(a, A));

    //    assertTrue(collator.equals(aUmlaut, aUmlaut.toLowerCase(locale)));
    //    assertTrue(collator.equals(normalized, normalized.toLowerCase(locale)));
    assertFalse(collator.equals(aUmlaut, normalized));
  }

}

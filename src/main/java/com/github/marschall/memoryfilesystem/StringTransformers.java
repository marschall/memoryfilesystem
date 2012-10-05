package com.github.marschall.memoryfilesystem;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Locale;

public final class StringTransformers {

  public static final StringTransformer IDENTIY = new IdentityTransformer();
  
  public static final StringTransformer MAC_OS = new MacOS();

  public StringTransformer caseInsensitive() {
    return caseInsensitive(Locale.getDefault());
  }

  public  static StringTransformer caseInsensitive(Locale locale) {
    return new CaseInsenstive(locale);
  }
  
  static final class MacOS implements StringTransformer {

    /**
     * {@inheritDoc}
     */
    @Override
    public String transform(String s) {
      // Wikipedia says MAC_OS uses NFD
      // http://en.wikipedia.org/wiki/HFS_Plus
      return Normalizer.normalize(s, Form.NFD);
    }
    
  }

  static final class CaseInsenstive implements StringTransformer {

    private final Locale locale;

    CaseInsenstive(Locale locale) {
      this.locale = locale;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String transform(String s) {
      return s.toUpperCase(this.locale);
    }

  }
  static final class IdentityTransformer implements StringTransformer {
    /**
     * {@inheritDoc}
     */
    @Override
    public String transform(String s) {
      return s;
    }

  }

}

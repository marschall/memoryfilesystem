package com.github.marschall.memoryfilesystem;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Locale;

public final class StringTransformers {

  public static final StringTransformer IDENTIY = new IdentityTransformer();

  public static final StringTransformer NFD = new NFD();

  public static final StringTransformer NFC = new NFC();

  public StringTransformer caseInsensitive() {
    return caseInsensitive(Locale.getDefault());
  }

  public  static StringTransformer caseInsensitive(Locale locale) {
    return new CaseInsenstive(locale);
  }

  public  static StringTransformer caseInsensitiveMacOSNative(Locale locale) {
    return new CaseInsenstiveMacOSNative(locale);
  }

  public  static StringTransformer caseInsensitiveMacOSJvm(Locale locale) {
    return new CaseInsenstiveMacOSJvm(locale);
  }

  static final class NFD implements StringTransformer {

    @Override
    public String transform(String s) {
      // Wikipedia says MAC_OS uses NFD
      // http://en.wikipedia.org/wiki/HFS_Plus
      return Normalizer.normalize(s, Form.NFD);
    }

  }

  static final class NFC implements StringTransformer {

    @Override
    public String transform(String s) {
      return Normalizer.normalize(s, Form.NFC);
    }

  }

  static final class CaseInsenstiveMacOSNative implements StringTransformer {

    private final Locale locale;

    CaseInsenstiveMacOSNative(Locale locale) {
      this.locale = locale;
    }

    @Override
    public String transform(String s) {
      return Normalizer.normalize(s, Form.NFD).toUpperCase(this.locale);
    }

  }

  static final class CaseInsenstiveMacOSJvm implements StringTransformer {

    private final Locale locale;

    CaseInsenstiveMacOSJvm(Locale locale) {
      this.locale = locale;
    }

    @Override
    public String transform(String s) {
      return Normalizer.normalize(s, Form.NFC).toUpperCase(this.locale);
    }

  }

  static final class CaseInsenstive implements StringTransformer {

    private final Locale locale;

    CaseInsenstive(Locale locale) {
      this.locale = locale;
    }

    @Override
    public String transform(String s) {
      return s.toUpperCase(this.locale);
    }

  }

  static final class IdentityTransformer implements StringTransformer {

    @Override
    public String transform(String s) {
      return s;
    }

  }

}

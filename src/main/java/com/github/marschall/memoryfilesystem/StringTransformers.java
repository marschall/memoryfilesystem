package com.github.marschall.memoryfilesystem;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Locale;
import java.util.Objects;

/**
 * Constant definitions for the standard {@link StringTransformer StringTransformers}.
 */
public final class StringTransformers {

  /**
   * Keeps a string as is.
   */
  public static final StringTransformer IDENTIY = new IdentityTransformer();

  /**
   * Normalizes a string using
   * <a href="https://en.wikipedia.org/wiki/Unicode_equivalence#Normal_forms">NFD</a>
   */
  public static final StringTransformer NFD = new NFD();

  /**
   * Normalizes a string using
   * <a href="https://en.wikipedia.org/wiki/Unicode_equivalence#Normal_forms">NFC</a>
   */
  public static final StringTransformer NFC = new NFC();

  /**
   * Creates a case insensitive transformer for the current locale.
   *
   * @return the transformer
   */
  public StringTransformer caseInsensitive() {
    return caseInsensitive(Locale.getDefault());
  }

  /**
   * Creates a case insensitive transformer for the given locale.
   *
   * @param locale the locale
   * @return the transformer
   */
  public  static StringTransformer caseInsensitive(Locale locale) {
    Objects.requireNonNull(locale);
    return new CaseInsenstive(locale);
  }

  /**
   * Creates a case insensitive transformer for native macOS (NFD) for
   * the given locale.
   *
   * @param locale the locale
   * @return the transformer
   */
  public  static StringTransformer caseInsensitiveMacOSNative(Locale locale) {
    return new CaseInsenstiveMacOSNative(locale);
  }

  /**
   * Creates a case insensitive transformer for macOS as presented by the
   * JVM (NFC) for the given locale.
   *
   * @param locale the locale
   * @return the transformer
   */
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

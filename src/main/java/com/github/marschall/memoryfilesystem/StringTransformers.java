package com.github.marschall.memoryfilesystem;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

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
    return new CaseInsensitive(locale);
  }

  /**
   * Creates a case insensitive transformer for native macOS (NFD) for
   * the given locale.
   *
   * @param locale the locale
   * @return the transformer
   */
  public  static StringTransformer caseInsensitiveMacOSNative(Locale locale) {
    return new CaseInsensitiveMacOSNative(locale);
  }

  /**
   * Creates a case insensitive transformer for macOS as presented by the
   * JVM (NFC) for the given locale.
   *
   * @param locale the locale
   * @return the transformer
   */
  public  static StringTransformer caseInsensitiveMacOSJvm(Locale locale) {
    return new CaseInsensitiveMacOSJvm(locale);
  }

  static final class NFD implements StringTransformer {

    @Override
    public String transform(String s) {
      // Wikipedia says MAC_OS uses NFD
      // http://en.wikipedia.org/wiki/HFS_Plus
      return Normalizer.normalize(s, Form.NFD);
    }

    @Override
    public int getRegexFlags() {
      return Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ;
    }

  }

  static final class NFC implements StringTransformer {

    @Override
    public String transform(String s) {
      return Normalizer.normalize(s, Form.NFC);
    }

    @Override
    public int getRegexFlags() {
      return Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ;
    }

  }

  static final class CaseInsensitiveMacOSNative implements StringTransformer {

    private final Locale locale;

    CaseInsensitiveMacOSNative(Locale locale) {
      this.locale = locale;
    }

    @Override
    public String transform(String s) {
      return Normalizer.normalize(s, Form.NFD).toUpperCase(this.locale);
    }

    @Override
    public int getRegexFlags() {
      return Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ;
    }

  }

  static final class CaseInsensitiveMacOSJvm implements StringTransformer {

    private final Locale locale;

    CaseInsensitiveMacOSJvm(Locale locale) {
      this.locale = locale;
    }

    @Override
    public String transform(String s) {
      return Normalizer.normalize(s, Form.NFC).toUpperCase(this.locale);
    }

    @Override
    public int getRegexFlags() {
      return Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ;
    }

  }

  static final class CaseInsensitive implements StringTransformer {

    private final Locale locale;

    CaseInsensitive(Locale locale) {
      this.locale = locale;
    }

    @Override
    public String transform(String s) {
      return s.toUpperCase(this.locale);
    }

    @Override
    public int getRegexFlags() {
      return Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    }

  }

  static final class IdentityTransformer implements StringTransformer {

    @Override
    public String transform(String s) {
      return s;
    }

    @Override
    public int getRegexFlags() {
      return 0;
    }

  }

}

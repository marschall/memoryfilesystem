package com.github.marschall.memoryfilesystem;

import java.util.regex.Pattern;

/**
 * Functional interface for transforming a string.
 *
 * <p>Used to implement case sensitivity and case preservation.</p>
 *
 * <p>Advanced users can implement this to obtain detailed control over
 * case sensitivity and case preservation. Custom instances have to be
 * passed as a configuration when building a file system.</p>
 */
public interface StringTransformer {

  /**
   * Transforms a string.
   *
   * @param s the string to transform, not {@code null}
   * @return the transformed string, not {@code null}
   */
  String transform(String s);

  /**
   * Returns the regex flags used to achieve a compatible regex behavior.
   *
   * @return the regex flags
   * @see Pattern#compile(String, int)
   */
  int getRegexFlags();

}

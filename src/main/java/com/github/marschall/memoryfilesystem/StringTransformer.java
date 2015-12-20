package com.github.marschall.memoryfilesystem;

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
  public String transform(String s);

}

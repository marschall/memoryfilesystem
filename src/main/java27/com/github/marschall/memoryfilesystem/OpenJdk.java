package com.github.marschall.memoryfilesystem;

final class OpenJdk {

  private OpenJdk() {
    throw new AssertionError("not instantiable");
  }

  /**
   * The object header size of an array.
   * <p>
   * A method rather than a constant to prevent constant folding by javac.
   */
  static int arrayHeaderSize() {
    // Two words (flags & class oop)
    // plus array size (64 bit + 32 bit on 64 bit, 2 *32 bit + 32 bit on 32 bit).
    return 8 + 4;
  }

}

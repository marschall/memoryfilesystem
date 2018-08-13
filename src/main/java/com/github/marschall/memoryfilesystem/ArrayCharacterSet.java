package com.github.marschall.memoryfilesystem;

class ArrayCharacterSet implements CharacterSet {

  private final char[] characters;

  ArrayCharacterSet(char[] characters) {
    this.characters = characters;
  }

  @Override
  public boolean containsAny(String s) {
    for (char character : this.characters) {
      if (s.indexOf(character) >= 0) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean contains(char c) {
    for (char each : this.characters) {
      if (each == c) {
        return true;
      }
    }
    return false;
  }

}

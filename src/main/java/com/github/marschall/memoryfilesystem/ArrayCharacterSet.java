package com.github.marschall.memoryfilesystem;

class ArrayCharacterSet implements CharacterSet {

  private final char[] characters;

  ArrayCharacterSet(char[] characters) {
    this.characters = characters;
  }

  @Override
  public boolean containsAny(String s) {
    for (int i = 0; i < this.characters.length; ++i) {
      if (s.indexOf(this.characters[i]) >= 0) {
        return true;
      }
    }
    return false;
  }

}

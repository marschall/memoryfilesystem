package com.github.marschall.memoryfilesystem;

enum EmptyCharacterSet implements CharacterSet {

  INSTANCE;

  @Override
  public boolean containsAny(String s) {
    return false;
  }

  @Override
  public boolean contains(char c) {
    return false;
  }

}

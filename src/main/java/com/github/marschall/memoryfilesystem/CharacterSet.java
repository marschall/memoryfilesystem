package com.github.marschall.memoryfilesystem;

interface CharacterSet {

  boolean containsAny(String s);

  boolean contains(char c);

}

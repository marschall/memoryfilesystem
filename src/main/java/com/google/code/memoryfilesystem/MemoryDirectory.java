package com.google.code.memoryfilesystem;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class MemoryDirectory extends MemoryEntry {

  private final ConcurrentMap<String, MemoryEntry> entries;
  
  MemoryDirectory() {
    this.entries = new ConcurrentHashMap<>();
  }
  
}

package com.google.code.memoryfilesystem;

import java.nio.file.Path;
import java.util.List;

interface PathParser {
  
  Path parse(List<Root> roots, String first, String... more);

}

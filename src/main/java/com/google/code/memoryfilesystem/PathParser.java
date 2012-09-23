package com.google.code.memoryfilesystem;

import java.nio.file.Path;

interface PathParser {
  
  Path parse(Iterable<Root> roots, String first, String... more);

}

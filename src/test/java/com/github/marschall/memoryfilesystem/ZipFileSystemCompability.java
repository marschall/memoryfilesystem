package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

public class ZipFileSystemCompability {

  @Test
  public void empty() throws URISyntaxException, IOException {
    Path outer = Files.createTempFile("sample", ".zip");
    try {
      Files.delete(outer);
      URI uri = new URI("jar:" + outer.toUri().toString());
      Map<String, ?> env = Collections.singletonMap("create", "true");
      Path path = null;
      try (FileSystem fs = FileSystems.newFileSystem(uri, env)) {
        path = fs.getPath("");
        System.out.println(path.toUri());
      }
      path.getFileSystem().isReadOnly();
      outer.endsWith(path);
    } finally {
      Files.delete(outer);
    }
  }

}

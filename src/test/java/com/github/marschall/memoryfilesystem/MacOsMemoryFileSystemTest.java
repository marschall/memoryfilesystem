package com.github.marschall.memoryfilesystem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Map;

import org.junit.Test;

public class MacOsMemoryFileSystemTest {

  @Test
  public void macOsNormalization() throws IOException {
    URI uri = URI.create("memory:uri");
    Map<String, ?> env = EnvironmentBuilder.newMacOs().build();
    try (FileSystem fileSystem = FileSystems.newFileSystem(uri, env)) {
      String aUmlaut = "\u00C4";
      Path aPath = fileSystem.getPath(aUmlaut);
      String normalized = Normalizer.normalize(aUmlaut, Form.NFD);
      Path nPath = fileSystem.getPath(normalized);
      
      Path createdFile = null;
      try { 
        createdFile = Files.createFile(aPath);
        assertEquals(1, createdFile.getFileName().toString().length());
        assertEquals(1, createdFile.toAbsolutePath().getFileName().toString().length());
        assertEquals(2, createdFile.toRealPath().getFileName().toString().length());
        
        assertTrue(Files.exists(aPath));
        assertTrue(Files.exists(nPath));
        assertTrue(Files.isSameFile(aPath, nPath));
        assertTrue(Files.isSameFile(nPath, aPath));
        assertThat(aPath, not(equalTo(nPath)));
      } finally {
        if (createdFile != null) {
          Files.delete(createdFile);
        }
      }
      
    }
    
  }
  
  @Test
  public void macOsComparison() throws IOException {
    URI uri = URI.create("memory:uri");
    Map<String, ?> env = EnvironmentBuilder.newMacOs().build();
    try (FileSystem fileSystem = FileSystems.newFileSystem(uri, env)) {
      Path aLower = fileSystem.getPath("a");
      Path aUpper = fileSystem.getPath("A");
      assertThat(aLower, not(equalTo(aUpper)));
      Path createdFile = null;
      try { 
        createdFile = Files.createFile(aLower);
        assertTrue(Files.exists(aLower));
        assertTrue(Files.exists(aUpper));
        assertTrue(Files.isSameFile(aLower, aUpper));
      } finally {
        if (createdFile != null) {
          Files.delete(createdFile);
        }
      }
    }
  }
  
  @Test
  public void macOsPaths() throws IOException {
    URI uri = URI.create("memory:uri");
    Map<String, ?> env = EnvironmentBuilder.newMacOs().build();
    try (FileSystem fileSystem = FileSystems.newFileSystem(uri, env)) {
      String aUmlaut = "\u00C4";
      String normalized = Normalizer.normalize(aUmlaut, Form.NFD);
      assertEquals(1, aUmlaut.length());
      assertEquals(2, normalized.length());
      Path aPath = fileSystem.getPath("/" + aUmlaut);
      Path nPath = fileSystem.getPath("/" + normalized);
      assertEquals(1, aPath.getName(0).toString().length());
      assertThat(aPath, not(equalTo(nPath)));
    }
  }
  
}

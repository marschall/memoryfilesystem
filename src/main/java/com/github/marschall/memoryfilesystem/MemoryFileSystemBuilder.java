package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MemoryFileSystemBuilder {

  private final List<String> roots;
  
  private final List<String> users;
  
  private final List<String> groups;

  private String separator;

  private String currentWorkingDirectory;
  
  private StringTransformer storeTransformer;
  
  private StringTransformer lookUpTransformer;
  
  private StringTransformer principalTransformer;
  
  private Collator collator;

  private Locale locale;
  
  private MemoryFileSystemBuilder() {
    this.roots = new ArrayList<>();
    this.users = new ArrayList<>();
    this.groups = new ArrayList<>();
  }

  public MemoryFileSystemBuilder addRoot(String root) {
    this.roots.add(root);
    return this;
  }

  public MemoryFileSystemBuilder setSeprator(String separator) {
    this.separator = separator;
    return this;
  }
  
  public MemoryFileSystemBuilder addUser(String userName) {
    this.users.add(userName);
    return this;
  }
  
  public MemoryFileSystemBuilder addGroup(String groupName) {
    this.groups.add(groupName);
    return this;
  }

  public MemoryFileSystemBuilder setCurrentWorkingDirectory(String currentWorkingDirectory) {
    this.currentWorkingDirectory = currentWorkingDirectory;
    return this;
  }
  
  public MemoryFileSystemBuilder setStoreTransformer(StringTransformer storeTransformer) {
    this.storeTransformer = storeTransformer;
    return this;
  }
  
  public MemoryFileSystemBuilder setLocale(Locale locale) {
    this.locale = locale;
    return this;
  }
  
  private Locale getLocale() {
    if (this.locale == null) {
      return Locale.getDefault();
    } else {
      return this.locale;
    }
  }
  
  public MemoryFileSystemBuilder setCaseSensitive(boolean caseSensitive) {
    if (caseSensitive) {
      this.lookUpTransformer = StringTransformers.IDENTIY;
      this.collator = MemoryFileSystemProperties.caseSensitiveCollator(this.getLocale());
    } else {
      this.lookUpTransformer = StringTransformers.caseInsensitive(this.getLocale());
      this.collator = MemoryFileSystemProperties.caseInsensitiveCollator(this.getLocale());

    }
    return this;
  }
  
  public MemoryFileSystemBuilder setCollator(Collator collator) {
    this.collator = collator;
    return this;
  }

  public static MemoryFileSystemBuilder newEmpty() {
    return new MemoryFileSystemBuilder();
  }
  
  public static MemoryFileSystemBuilder newUnix() {
    return new MemoryFileSystemBuilder()
    .addRoot(MemoryFileSystemProperties.UNIX_ROOT)
    .setSeprator(MemoryFileSystemProperties.UNIX_SEPARATOR)
    .addUser(getSystemUserName())
    .addGroup(getSystemUserName())
    .setCurrentWorkingDirectory("/home/" + getSystemUserName())
    .setStoreTransformer(StringTransformers.IDENTIY)
    .setCaseSensitive(true);
  }
  
  public static MemoryFileSystemBuilder newMacOs() {
    return new MemoryFileSystemBuilder()
    .addRoot(MemoryFileSystemProperties.UNIX_ROOT)
    .setSeprator(MemoryFileSystemProperties.UNIX_SEPARATOR)
    .addUser(getSystemUserName())
    .addGroup(getSystemUserName())
    .setCurrentWorkingDirectory("/Users/" + getSystemUserName())
    .setStoreTransformer(StringTransformers.MAC_OS)
    .setCaseSensitive(true);
  }

  public static MemoryFileSystemBuilder newWindows() {
    return new MemoryFileSystemBuilder()
      .addRoot("C:\\")
      .setSeprator(MemoryFileSystemProperties.WINDOWS_SEPARATOR)
      .addUser(getSystemUserName())
      .addGroup(getSystemUserName())
      .setCurrentWorkingDirectory("C:\\Users\\" + getSystemUserName())
      .setStoreTransformer(StringTransformers.IDENTIY)
      .setCaseSensitive(false);
  }

  static String getSystemUserName() {
    return System.getProperty("user.name");
  }
  
  public FileSystem build(String name) throws IOException {
    Map<String, ?> env = buildEnvironment();
    URI uri = URI.create("memory:".concat(name));
    ClassLoader classLoader = MemoryFileSystemBuilder.class.getClassLoader();
    return FileSystems.newFileSystem(uri, env, classLoader);
  }

  public Map<String, ?> buildEnvironment() {
    Map<String, Object> env = new HashMap<>();
    if (!this.roots.isEmpty()) {
      env.put(MemoryFileSystemProperties.ROOTS_PROPERTY, this.roots);
    }
    if (this.separator != null) {
      env.put(MemoryFileSystemProperties.DEFAULT_NAME_SEPARATOR_PROPERTY, this.separator);
    }
    if (this.currentWorkingDirectory != null) {
      env.put(MemoryFileSystemProperties.CURRENT_WORKING_DIRECTORY_PROPERTY, this.currentWorkingDirectory);
    }
    if (this.storeTransformer != null) {
      env.put(MemoryFileSystemProperties.PATH_STORE_TRANSFORMER_PROPERTY, this.storeTransformer);
    }
    if (this.lookUpTransformer != null) {
      env.put(MemoryFileSystemProperties.PATH_LOOKUP_TRANSFORMER_PROPERTY, this.lookUpTransformer);
    }
    if (this.principalTransformer != null) {
      env.put(MemoryFileSystemProperties.PRINCIPAL_TRANSFORMER_PROPERTY, this.principalTransformer);
    }
    if (this.collator != null) {
      env.put(MemoryFileSystemProperties.COLLATOR_PROPERTY, this.collator);
    }
    return env;
  }

}

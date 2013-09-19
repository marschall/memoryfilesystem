package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class MemoryFileSystemBuilder {

  private final List<String> roots;

  private final Set<String> users;

  private final Set<String> groups;

  private final Set<Character> forbiddenCharacters;

  private final Set<String> additionalFileAttributeViews;

  private Set<PosixFilePermission> umask;

  private String separator;

  private String currentWorkingDirectory;

  private StringTransformer storeTransformer;

  private StringTransformer lookUpTransformer;

  private StringTransformer principalTransformer;

  private Collator collator;

  private Locale locale;

  private MemoryFileSystemBuilder() {
    this.roots = new ArrayList<>();
    this.users = new LinkedHashSet<>();
    this.groups = new LinkedHashSet<>();
    this.additionalFileAttributeViews = new HashSet<>();
    this.forbiddenCharacters = new HashSet<>();
  }

  public MemoryFileSystemBuilder addRoot(String root) {
    this.roots.add(root);
    return this;
  }

  public MemoryFileSystemBuilder setSeprator(String separator) {
    this.separator = separator;
    return this;
  }

  public MemoryFileSystemBuilder addForbiddenCharacter(char c) {
    this.forbiddenCharacters.add(c);
    return this;
  }

  public MemoryFileSystemBuilder addUser(String userName) {
    this.users.add(userName);
    this.addGroup(userName);
    return this;
  }

  /**
   * Sets the permissions that will be applied to new files.
   * 
   * @param umask the permissions that will be applied to new files
   * @return the receiver
   */
  public MemoryFileSystemBuilder setUmask(Set<PosixFilePermission> umask) {
    this.umask = umask;
    return this;
  }

  public MemoryFileSystemBuilder addGroup(String groupName) {
    this.groups.add(groupName);
    return this;
  }

  // can't add "owner" directly"
  public MemoryFileSystemBuilder addFileAttributeView(String fileAttributeViewName) {
    if (FileAttributeViews.isSupported(fileAttributeViewName)) {
      if (!FileAttributeViews.BASIC.equals(fileAttributeViewName)) {
        // ignore "basic", always supported
        this.additionalFileAttributeViews.add(fileAttributeViewName);
      }
    } else {
      throw new IllegalArgumentException("file attribute view \"" + fileAttributeViewName + "\" is not supported");
    }
    return this;
  }

  // can't add FileOwnerAttributeView directly
  public MemoryFileSystemBuilder addFileAttributeView(Class<? extends FileAttributeView> fileAttributeView) {
    return this.addFileAttributeView(FileAttributeViews.mapAttributeView(fileAttributeView));
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

  public MemoryFileSystemBuilder setLookUpTransformer(StringTransformer lookUpTransformer) {
    this.lookUpTransformer = lookUpTransformer;
    return this;
  }

  public MemoryFileSystemBuilder setCollator(Collator collator) {
    this.collator = collator;
    return this;
  }

  public static MemoryFileSystemBuilder newEmpty() {
    return new MemoryFileSystemBuilder();
  }

  public static MemoryFileSystemBuilder newLinux() {
    return new MemoryFileSystemBuilder()
    .addRoot(MemoryFileSystemProperties.UNIX_ROOT)
    .setSeprator(MemoryFileSystemProperties.UNIX_SEPARATOR)
    .addUser(getSystemUserName())
    .addGroup(getSystemUserName())
    .addFileAttributeView(PosixFileAttributeView.class)
    .setCurrentWorkingDirectory("/home/" + getSystemUserName())
    .setStoreTransformer(StringTransformers.IDENTIY)
    .setCaseSensitive(true);
  }

  public static MemoryFileSystemBuilder newMacOs() {
    MemoryFileSystemBuilder builder = new MemoryFileSystemBuilder();
    return builder
            .addRoot(MemoryFileSystemProperties.UNIX_ROOT)
            .setSeprator(MemoryFileSystemProperties.UNIX_SEPARATOR)
            .addUser(getSystemUserName())
            .addGroup(getSystemUserName())
            .addFileAttributeView(PosixFileAttributeView.class)
            .setCurrentWorkingDirectory("/Users/" + getSystemUserName())
            .setCollator(MemoryFileSystemProperties.caseSensitiveCollator(builder.getLocale()))
            .setLookUpTransformer(StringTransformers.caseInsensitiveMacOS(builder.getLocale()))
            .setStoreTransformer(StringTransformers.MAC_OS)
            .addForbiddenCharacter((char) 0);
  }

  public static MemoryFileSystemBuilder newWindows() {
    return new MemoryFileSystemBuilder()
    .addRoot("C:\\")
    .setSeprator(MemoryFileSystemProperties.WINDOWS_SEPARATOR)
    .addUser(getSystemUserName())
    .addGroup(getSystemUserName())
    .addFileAttributeView(DosFileAttributeView.class)
    .setCurrentWorkingDirectory("C:\\Users\\" + getSystemUserName())
    .setStoreTransformer(StringTransformers.IDENTIY)
    .setCaseSensitive(false)
    // TODO check for 0x00
    .addForbiddenCharacter('\\')
    .addForbiddenCharacter('/')
    .addForbiddenCharacter(':')
    .addForbiddenCharacter('*')
    .addForbiddenCharacter('?')
    .addForbiddenCharacter('"')
    .addForbiddenCharacter('<')
    .addForbiddenCharacter('>')
    .addForbiddenCharacter('|');
  }

  static String getSystemUserName() {
    return System.getProperty("user.name");
  }

  public FileSystem build(String name) throws IOException {
    Map<String, ?> env = this.buildEnvironment();
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
    if (this.additionalFileAttributeViews != null) {
      env.put(MemoryFileSystemProperties.FILE_ATTRIBUTE_VIEWS_PROPERTY, this.additionalFileAttributeViews);
    }
    if (this.umask != null) {
      env.put(MemoryFileSystemProperties.UMASK_PROPERTY, this.umask);
    }
    if (this.forbiddenCharacters != null) {
      env.put(MemoryFileSystemProperties.FORBIDDEN_CHARACTERS, this.forbiddenCharacters);
    }
    return env;
  }

}

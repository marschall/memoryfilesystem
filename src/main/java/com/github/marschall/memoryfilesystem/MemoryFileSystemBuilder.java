package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <a href="https://en.wikipedia.org/wiki/Builder_pattern">Builder</a>
 * for conveniently creating create memory file system instances.
 *
 * <p>The builder takes care of creating the environment and selecting
 * the correct class loader to pass to {@link FileSystems#newFileSystem(URI, Map, ClassLoader)}.</p>
 */
public final class MemoryFileSystemBuilder {

  /**
   * Prefix for generating file system names.
   */
  private static final String UNIQUE_NAME_PREFIX = "__g_";

  /**
   * Counter used for generating unique names.
   */
  private static final AtomicLong UNIQUE_COUNTER = new AtomicLong();

  private final List<String> roots;

  private final Set<String> users;

  private final Set<String> groups;

  private final Set<Character> forbiddenCharacters;

  private final Set<String> additionalFileAttributeViews;

  private Set<PosixFilePermission> umask = Collections.emptySet();

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

  /**
   * Add a file system root.
   *
   * <p>This method is intended to be used in Windows mode to add a drive
   * letter eg.:</p>
   *
   * <pre><code>builder.addRoot("D:\\")</code></pre>
   *
   * @param root the file system root
   * @return the current builder object
   */
  public MemoryFileSystemBuilder addRoot(String root) {
    Objects.requireNonNull(root);
    this.roots.add(root);
    return this;
  }

  /**
   * Sets the the name separator.
   *
   * @param separator the name separator, not {@code null}
   * @return the current builder object
   * @see java.nio.file.FileSystem#getSeparator()
   */
  public MemoryFileSystemBuilder setSeprator(String separator) {
    Objects.requireNonNull(separator);
    this.separator = separator;
    return this;
  }

  /**
   * Forbids a character to be used in a name.
   *
   * @param c the character to forbid
   * @return the current builder object
   */
  public MemoryFileSystemBuilder addForbiddenCharacter(char c) {
    this.forbiddenCharacters.add(c);
    return this;
  }

  /**
   * Adds a user and a group to the file systems
   * {@link java.nio.file.attribute.UserPrincipalLookupService}.
   *
   * @param userName the name of the user to add
   * @return the current builder object
   */
  public MemoryFileSystemBuilder addUser(String userName) {
    this.users.add(userName);
    this.addGroup(userName);
    return this;
  }


  /**
   * Adds a group to the file systems
   * {@link java.nio.file.attribute.UserPrincipalLookupService}.
   *
   * @param groupName the name of the group to add
   * @return the current builder object
   */
  public MemoryFileSystemBuilder addGroup(String groupName) {
    this.groups.add(groupName);
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

  /**
   * Adds support for an attribute view.
   *
   * @param fileAttributeViewName the name of the attribute view to add
   * @return the current builder object
   * @see java.nio.file.attribute.AttributeView#name()
   */
  // can't add "owner" directly"
  public MemoryFileSystemBuilder addFileAttributeView(String fileAttributeViewName) {
    Objects.requireNonNull(fileAttributeViewName);
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

  /**
   * Adds support for an attribute view.
   *
   * @param fileAttributeView the attribute view class
   * @return the current builder object
   */
  // can't add FileOwnerAttributeView directly
  public MemoryFileSystemBuilder addFileAttributeView(Class<? extends FileAttributeView> fileAttributeView) {
    return this.addFileAttributeView(FileAttributeViews.mapAttributeView(fileAttributeView));
  }

  /**
   * Sets the current working directory used for resolving relative paths.
   *
   * @param currentWorkingDirectory path of the current working directory
   * @return the current builder object
   */
  public MemoryFileSystemBuilder setCurrentWorkingDirectory(String currentWorkingDirectory) {
    Objects.requireNonNull(currentWorkingDirectory);
    this.currentWorkingDirectory = currentWorkingDirectory;
    return this;
  }

  /**
   * Sets the transformer that controls how a file name is stored.
   *
   * @param storeTransformer to apply to the file names before storing
   * @return the current builder object
   */
  public MemoryFileSystemBuilder setStoreTransformer(StringTransformer storeTransformer) {
    Objects.requireNonNull(storeTransformer);
    this.storeTransformer = storeTransformer;
    return this;
  }

  /**
   * Sets the locale to be used for case insensitivity.
   *
   * @param locale for case insensitivity
   * @return the current builder object
   * @see #setCaseSensitive(boolean)
   */
  public MemoryFileSystemBuilder setLocale(Locale locale) {
    Objects.requireNonNull(locale);
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

  /**
   * Toggles case sensitivity.
   *
   * @param caseSensitive whether the file names should be case sensitive
   * @return the current builder object
   * @see #setLocale(Locale)
   */
  public MemoryFileSystemBuilder setCaseSensitive(boolean caseSensitive) {
    if (caseSensitive) {
      this.lookUpTransformer = StringTransformers.IDENTIY;
      this.collator = MemoryFileSystemProperties.caseSensitiveCollator(this.getLocale(), false);
    } else {
      this.lookUpTransformer = StringTransformers.caseInsensitive(this.getLocale());
      this.collator = MemoryFileSystemProperties.caseInsensitiveCollator(this.getLocale());

    }
    return this;
  }

  /**
   * Sets the transformer that controls how a file name is looked up.
   *
   * @param lookUpTransformer to apply to the file names for lookup
   * @return the current builder object
   */
  public MemoryFileSystemBuilder setLookUpTransformer(StringTransformer lookUpTransformer) {
    this.lookUpTransformer = lookUpTransformer;
    return this;
  }

  /**
   * Sets the collator used for name comparisons.
   *
   * @param collator the collator for name comparisons
   * @return the current builder object
   */
  public MemoryFileSystemBuilder setCollator(Collator collator) {
    Objects.requireNonNull(collator);
    this.collator = collator;
    return this;
  }

  /**
   * Creates a builder for a very basic file system.
   *
   * <p>The file system does not support permissions and only supports
   * {@link BasicFileAttributeView}. It is UNIX-like in the sense that
   * is uses {@literal "/"} as a separator, has a single root and is
   * case sensitive and case preserving.<p>
   *
   * @return the builder
   */
  public static MemoryFileSystemBuilder newEmpty() {
    return new MemoryFileSystemBuilder();
  }

  /**
   * Creates a builder for a Linux-like file system.
   *
   * <p>The file system has the following properties:</p>
   * <ul>
   *  <li>the root is {@value MemoryFileSystemProperties#UNIX_ROOT}</li>
   *  <li>the separator is {@value MemoryFileSystemProperties#UNIX_SEPARATOR}</li>
   *  <li>the current user (value of the {@code "user.name"} system property)
   *  is added as a user</li>
   *  <li>the current user (value of the {@code "user.name"} system property)
   *  is added as a group</li>
   *  <li>{@link PosixFileAttributeView} is added as an attribute view</li>
   *  <li>the current working directory is {@code "/home/${user.name}"}</li>
   *  <li>the file system is case sensitive</li>
   *  <li>{@code 0x00} is not allowed in file names</li>
   * </ul>
   *
   * @return the builder
   */
  public static MemoryFileSystemBuilder newLinux() {
    return new MemoryFileSystemBuilder()
            .addRoot(MemoryFileSystemProperties.UNIX_ROOT)
            .setSeprator(MemoryFileSystemProperties.UNIX_SEPARATOR)
            .addUser(getSystemUserName())
            .addGroup(getSystemUserName())
            .addFileAttributeView(PosixFileAttributeView.class)
            .setCurrentWorkingDirectory("/home/" + getSystemUserName())
            .setStoreTransformer(StringTransformers.IDENTIY)
            .setCaseSensitive(true)
            .addForbiddenCharacter((char) 0);
  }

  /**
   * Creates a builder for a macOS-like file system.
   *
   * <p>The file system has the following properties:</p>
   * <ul>
   *  <li>the root is {@value MemoryFileSystemProperties#UNIX_ROOT}</li>
   *  <li>the separator is {@value MemoryFileSystemProperties#UNIX_SEPARATOR}</li>
   *  <li>the current user (value of the {@code "user.name"} system property)
   *  is added as a user</li>
   *  <li>the current user (value of the {@code "user.name"} system property)
   *  is added as a group</li>
   *  <li>{@link PosixFileAttributeView} is added as an attribute view</li>
   *  <li>the current working directory is {@code "/Users/${user.name}"}</li>
   *  <li>the file system is case insensitive and case case preserving</li>
   *  <li>file names are normalized to NFC</li>
   *  <li>{@code 0x00} is not allowed in file names</li>
   * </ul>
   *
   * @return the builder
   */
  public static MemoryFileSystemBuilder newMacOs() {
    // new JVMs use NFC instead of the native NFD
    MemoryFileSystemBuilder builder = new MemoryFileSystemBuilder();
    return builder
            .addRoot(MemoryFileSystemProperties.UNIX_ROOT)
            .setSeprator(MemoryFileSystemProperties.UNIX_SEPARATOR)
            .addUser(getSystemUserName())
            .addGroup(getSystemUserName())
            .addFileAttributeView(PosixFileAttributeView.class)
            .setCurrentWorkingDirectory("/Users/" + getSystemUserName())
            .setCollator(MemoryFileSystemProperties.caseSensitiveCollator(builder.getLocale(), true))
            .setLookUpTransformer(StringTransformers.caseInsensitiveMacOSJvm(builder.getLocale()))
            .setStoreTransformer(StringTransformers.NFC)
            .addForbiddenCharacter((char) 0);
  }

  /**
   * Creates a builder for a macOS-like file system for old JVMs.
   *
   * <p>The file system has the following properties:</p>
   * <ul>
   *  <li>the root is {@value MemoryFileSystemProperties#UNIX_ROOT}</li>
   *  <li>the separator is {@value MemoryFileSystemProperties#UNIX_SEPARATOR}</li>
   *  <li>the current user (value of the {@code "user.name"} system property)
   *  is added as a user</li>
   *  <li>the current user (value of the {@code "user.name"} system property)
   *  is added as a group</li>
   *  <li>{@link PosixFileAttributeView} is added as an attribute view</li>
   *  <li>the current working directory is {@code "/Users/${user.name}"}</li>
   *  <li>the file system is case insensitive and case case preserving</li>
   *  <li>file names are normalized to NFD</li>
   *  <li>{@code 0x00} is not allowed in file names</li>
   * </ul>
   *
   * @return the builder
   */
  public static MemoryFileSystemBuilder newMacOsOldJvm() {
    // old JVMs used the native NFC
    MemoryFileSystemBuilder builder = new MemoryFileSystemBuilder();
    return builder
            .addRoot(MemoryFileSystemProperties.UNIX_ROOT)
            .setSeprator(MemoryFileSystemProperties.UNIX_SEPARATOR)
            .addUser(getSystemUserName())
            .addGroup(getSystemUserName())
            .addFileAttributeView(PosixFileAttributeView.class)
            .setCurrentWorkingDirectory("/Users/" + getSystemUserName())
            .setCollator(MemoryFileSystemProperties.caseSensitiveCollator(builder.getLocale(), false))
            .setLookUpTransformer(StringTransformers.caseInsensitiveMacOSNative(builder.getLocale()))
            .setStoreTransformer(StringTransformers.NFD)
            .addForbiddenCharacter((char) 0);
  }
  /**
   * Creates a builder for a Windows-like file system.
   *
   * <p>The file system has the following properties:</p>
   * <ul>
   *  <li>the root is {@code "C:\\"}</li>
   *  <li>the separator is {@value MemoryFileSystemProperties#WINDOWS_SEPARATOR}</li>
   *  <li>the current user (value of the {@code "user.name"} system property)
   *  is added as a user</li>
   *  <li>the current user (value of the {@code "user.name"} system property)
   *  is added as a group</li>
   *  <li>{@link DosFileAttributeView} is added as an attribute view</li>
   *  <li>the current working directory is {@code "C:\\Users\\${user.name}"}</li>
   *  <li>the file system is case insensitive and case case preserving</li>
   *  <li>{@code '\\'}, {@code '/'}, {@code ':'}, {@code '*'}, {@code '?'},
   *  {@code '"'}, {@code '<'}, {@code '<'} and {@code '|'} and not allowed
   *  in file names</li>
   * </ul>
   *
   * @return the builder
   */
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
            // TODO forbid
            // CON, PRN, AUX, CLOCK$, NULL
            // COM1, COM2, COM3, COM4, COM5, COM6, COM7, COM8, COM9
            // LPT1, LPT2, LPT3, LPT4, LPT5, LPT6, LPT7, LPT8, and LPT9
            // TODO forbid
            // $Mft, $MftMirr, $LogFile, $Volume, $AttrDef, $Bitmap, $Boot, $BadClus, $Secure,
            // $Upcase, $Extend, $Quota, $ObjId and $Reparse
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

  /**
   * Creates the new file system instance.
   *
   * @param name the name, must be unique otherwise a
   *  {@link FileSystemAlreadyExistsException} will be thrown
   * @return the file system
   * @throws IOException if the file system can't be created
   * @see FileSystems#newFileSystem(URI, Map, ClassLoader)
   */
  public FileSystem build(String name) throws IOException {
    Map<String, ?> env = this.buildEnvironment();
    URI uri = URI.create("memory:".concat(name));
    ClassLoader classLoader = MemoryFileSystemBuilder.class.getClassLoader();
    return FileSystems.newFileSystem(uri, env, classLoader);
  }

  /**
   * Creates the new file system instance.
   *
   * @return the file system
   * @throws IOException if the file system can't be created
   * @see FileSystems#newFileSystem(URI, Map, ClassLoader)
   */
  public FileSystem build() throws IOException {
    return this.build(UNIQUE_NAME_PREFIX + UNIQUE_COUNTER.incrementAndGet());
  }

  /**
   * Builds an environment to pass to {@link FileSystems#newFileSystem(URI, Map)}.
   *
   * @return the environment
   */
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
    if (!this.users.isEmpty()) {
      env.put(MemoryFileSystemProperties.USERS_PROPERTY, new ArrayList<>(this.users));
    }
    if (!this.groups.isEmpty()) {
      env.put(MemoryFileSystemProperties.GROUPS_PROPERTY, new ArrayList<>(this.groups));
    }
    return env;
  }

}

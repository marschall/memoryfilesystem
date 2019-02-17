package com.github.marschall.memoryfilesystem;

import java.nio.file.Path;
import java.text.Collator;
import java.time.temporal.TemporalUnit;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Constant definitions for configuration parameters for creating memory file systems.
 *
 * <p>Whenever possible using {@link MemoryFileSystemBuilder} is recommended.</p>
 */
public class MemoryFileSystemProperties {

  static final String UNIX_SEPARATOR = "/";

  static final String WINDOWS_SEPARATOR = "\\";

  /**
   * The resolution of the Windows file system, 100ns.
   */
  public static final TemporalUnit WINDOWS_RESOLUTION = new HundredNanoseconds();

  /**
   * The default separator of path elements.
   */
  public static final String DEFAULT_NAME_SEPARATOR = UNIX_SEPARATOR;

  /**
   * Name of the property of the separator of path elements. Must be a {@link String}.
   */
  public static final String DEFAULT_NAME_SEPARATOR_PROPERTY = "file.separator";

  /**
   * Name of the property of the current working directory. Must be a {@link String}
   * and an absolute path.
   */
  public static final String CURRENT_WORKING_DIRECTORY_PROPERTY = "user.dir";

  /**
   * Name of the property for the file system roots, aka drive letters.
   * Must be a {@code List<String>} with the like values having the following
   * form {@code "C:\\"}.
   *
   * Only supported for Windows file systems.
   */
  public static final String ROOTS_PROPERTY = "roots";

  /**
   * Name of the property names of the attribute view supported by the file system
   * besides {@code "basic"}.
   * Must be a {@code Set<String>}.
   *
   * @see java.nio.file.attribute.AttributeView#name()
   */
  public static final String FILE_ATTRIBUTE_VIEWS_PROPERTY = "file.attrs";

  /**
   * Name of the property holding the characters not allowed in file names.
   * Must be a {@code Set<Character>}.
   */
  public static final String FORBIDDEN_CHARACTERS_PROPERTY = "file.name.forbidden";

  /**
   * Name of the property holding the users supported by the file system.
   * Must be a {@code List<String>}.
   */
  public static final String USERS_PROPERTY = "users";

  /**
   * Name of the property holding the groups supported by the file system.
   * Must be a {@code List<String>}.
   */
  public static final String GROUPS_PROPERTY = "groups";

  /**
   * Name of the property of the {@link StringTransformer} used to store files
   * in a directory.
   */
  public static final String PATH_STORE_TRANSFORMER_PROPERTY = "path.store.transformer";

  /**
   * Name of the property of the {@link StringTransformer} used to look up files
   * in a directory.
   */
  public static final String PATH_LOOKUP_TRANSFORMER_PROPERTY = "path.lookup.transformer";

  /**
   * Name of the property of the {@link StringTransformer} used to look up users.
   */
  public static final String PRINCIPAL_TRANSFORMER_PROPERTY = "principal.transformer";

  /**
   * Name of property for the resolution of the file time used for modification, access and creation time.
   *
   * Must be an implementation of {@link java.time.temporal.TemporalUnit}.
   *
   * @see java.time.Instant#truncatedTo(java.time.temporal.TemporalUnit)
   */
  public static final String FILE_TIME_RESOLUTION_PROPERTY = "file.time.resolution";

  /**
   * Name of the property of the {@link Collator} used to compare path elements.
   *
   * @see java.nio.file.Path#compareTo(Path)
   */
  public static final String COLLATOR_PROPERTY = "collator";

  /**
   * Name of the property of the <a href="https://en.wikipedia.org/wiki/Umask">umask</a>.
   * The umask is a set of permissions that will be removed from newly created files.
   *
   * Must be a {@code Set<PosixFilePermission>}.
   */
  public static final String UMASK_PROPERTY = "file.umask";

  static final String UNIX_ROOT = "/";

  static final List<String> DEFAULT_ROOTS = Collections.singletonList(UNIX_ROOT);

  // single entry caches for collators
  // profiling has shown that getting collators is a bottleneck
  private static final AtomicReference<CollatorCache> INSENSITIVE_COLLATOR = new AtomicReference<>();
  private static final AtomicReference<CollatorCache> DECOMPOSITION_COLLATOR = new AtomicReference<>();
  private static final AtomicReference<CollatorCache> NO_DECOMPOSITION_COLLATOR = new AtomicReference<>();

  static Collator caseSensitiveCollator(Locale locale, boolean decomposition) {
    AtomicReference<CollatorCache> reference = decomposition ? DECOMPOSITION_COLLATOR : NO_DECOMPOSITION_COLLATOR;
    int decompositionMode = decomposition ? Collator.CANONICAL_DECOMPOSITION : Collator.NO_DECOMPOSITION;
    return getCaseSensitiveCollatorFromCache(locale, reference, decompositionMode);
  }

  private static Collator getCaseSensitiveCollatorFromCache(Locale locale, AtomicReference<CollatorCache> reference, int decompositionMode) {
    CollatorCache cache = reference.get();
    if (cache == null || !cache.locale.equals(locale)) {
      Collator collator = Collator.getInstance(locale);
      collator.setDecomposition(decompositionMode);
      collator.setStrength(Collator.IDENTICAL);
      reference.set(new CollatorCache(locale, collator));
      return collator;
    } else {
      return cache.collator;
    }

  }

  static Collator caseInsensitiveCollator(Locale locale) {
    CollatorCache cache = INSENSITIVE_COLLATOR.get();
    if (cache == null || !cache.locale.equals(locale)) {
      Collator collator = Collator.getInstance(locale);
      collator.setDecomposition(Collator.NO_DECOMPOSITION);
      collator.setStrength(Collator.SECONDARY);

      INSENSITIVE_COLLATOR.set(new CollatorCache(locale, collator));

      return collator;

    } else {
      return cache.collator;
    }
  }

  static final class CollatorCache {

    final Locale locale;
    final Collator collator;
    CollatorCache(Locale locale, Collator collator) {
      this.locale = locale;
      this.collator = collator;
    }

  }

}

package com.github.marschall.memoryfilesystem;

import java.text.Collator;
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

  public static final String DEFAULT_NAME_SEPARATOR = UNIX_SEPARATOR;

  public static final String DEFAULT_NAME_SEPARATOR_PROPERTY = "file.separator";

  public static final String CURRENT_WORKING_DIRECTORY_PROPERTY = "user.dir";

  public static final String ROOTS_PROPERTY = "roots";

  public static final String FILE_ATTRIBUTE_VIEWS_PROPERTY = "file.attrs";

  public static final String FORBIDDEN_CHARACTERS = "file.name.forbidden";

  public static final String USERS_PROPERTY = "users";

  public static final String GROUPS_PROPERTY = "groups";

  public static final String PATH_STORE_TRANSFORMER_PROPERTY = "path.store.transformer";

  public static final String PATH_LOOKUP_TRANSFORMER_PROPERTY = "path.lookup.transformer";

  public static final String PRINCIPAL_TRANSFORMER_PROPERTY = "principal.transformer";

  public static final String COLLATOR_PROPERTY = "collator";

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

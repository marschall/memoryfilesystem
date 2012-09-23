package com.github.marschall.memoryfilesystem;

import java.lang.reflect.Field;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides means of uninstalling a memory file system provider.
 * 
 * <p>Uninstalling a file system provider is not intended by </p>
 * 
 * <p>See <a href="http://www.bracha.org/classloaders.ps">Dynamic Class Loading
 * in the Java&trade; Virtual Machine</a> for a good introduction into class loaders.
 * </p>
 *
 */
public final class MemoryFileSystemUninstaller {
  
  //REVIEW close all file systems?
  
  private MemoryFileSystemUninstaller() {
    throw new AssertionError("not instantiable");
  }
  
  /**
   * Checks whether a memory file system provider loaded by this classes
   * class loader is currently installed.
   * 
   * <p>This method does not check for memory file system providers loaded
   * by other class loaders. You do not want to .</p>
   * 
   * @return {@code true} if a memory file system provider is currently
   *  installed, {@code false} else
   */
  public static boolean isInstalled() {
    ClassLoader ownClassLoader = getOwnClassLoader();
    for (FileSystemProvider provider : FileSystemProvider.installedProviders()) {
      if (provider.getClass().getClassLoader() == ownClassLoader) {
        return true;
      }
    }
    return false;
  }
  
  private static ClassLoader getOwnClassLoader() {
    return MemoryFileSystemUninstaller.class.getClassLoader();
  }
  
  private static void uninstall(FileSystemProvider provider) {
    Class<?> providerClass = FileSystemProvider.class;
    try {
      Field installedProvidersField = providerClass.getDeclaredField("installedProviders");
      installedProvidersField.setAccessible(true);
      Field lockField = providerClass.getDeclaredField("lock");
      lockField.setAccessible(true);
      // FileSystemProvider synchronizes on this so we do as well
      synchronized (lockField.get(null)) {
        // FileSystemProvider#installedProviders ins a Collections#UnmodifiableRandomAccessList
        Object unmodifiableInstalledProviders = installedProvidersField.get(null);
        Field modifiableInstalledProvidersField = unmodifiableInstalledProviders.getClass().getSuperclass().getDeclaredField("list");
        modifiableInstalledProvidersField.setAccessible(true);
        List<?> modifiableInstalledProviders = (List<?>) modifiableInstalledProvidersField.get(unmodifiableInstalledProviders);
        modifiableInstalledProviders.remove(provider);
      }
    } catch (ReflectiveOperationException e) {
      throw new UninstallationFailedException("uninstallation failed", e);
    }
    
  }
  
  /**
   * Tries to uninstall the memory file system provider loaded by this classes
   * class loader.
   * 
   * <p>This method may fail and throw a {@link UninstallationFailedException}
   * various reasons including but not limited to:</p>
   * <ul>
   *  <li>you run on a non-HotSpot JDK which has a different
   *    {@link FileSystemProvider} implementation</li>
   *  <li>you run on a HotSpot JDK which wasn't release when this code was
   *    written and as a different implementation of
   *    {@link FileSystemProvider}</li>
   *  <li>you put restrictions on which reflective actions code can take</li>
   *  <li>the code (in this class) is hacky and brittle</li>
   * </ul>
   * 
   * @see #isInstalled()
   * @throws UninstallationFailedException if uninstallation failed
   * @return {@code true} if a provider was uninstalled, {@code false} if no
   *  provider to uninstall was found
   */
  public static boolean uninstall() {
    ClassLoader ownClassLoader = getOwnClassLoader();
    List<FileSystemProvider> toUninstall = new ArrayList<>(1);
    for (FileSystemProvider provider : FileSystemProvider.installedProviders()) {
      if (provider.getClass().getClassLoader() == ownClassLoader) {
        // don't call #uninstall(FileSystemProvider) because we can't remove
        // from a collection while iterating over it
        toUninstall.add(provider);
      }
    }
    for (FileSystemProvider provider : toUninstall) {
      uninstall(provider);
    }
    return !toUninstall.isEmpty();
  }

}

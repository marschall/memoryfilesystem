/**
 * 
 */
package com.github.marschall.memoryfilesystem;

/**
 * Signals that the uninstallation of the memory file system has failed.
 *
 * @see MemoryFileSystemUninstaller#uninstall()
 */
public final class UninstallationFailedException extends RuntimeException {

  UninstallationFailedException() {
    super();
  }

  UninstallationFailedException(String message) {
    super(message);
  }

  UninstallationFailedException(Throwable cause) {
    super(cause);
  }

  UninstallationFailedException(String message, Throwable cause) {
    super(message, cause);
  }

}

/**
 * 
 */
package com.google.code.memoryfilesystem;

/**
 * Signals that the uninstallation of the memory file system has failed.
 *
 * @see MemoryFileSystemUninstaller#uninstall()
 */
public final class UninstallationFailedException extends RuntimeException {

  private static final long serialVersionUID = -2429997282753129531L;

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

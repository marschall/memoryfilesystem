package com.github.marschall.memoryfilesystem;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.prefs.Preferences;

/**
 * Checks if the current user is the local administrator
 * <p>
 * Adapted from <a href="https://stackoverflow.com/a/23538961/1082681">this Stack Overflow answer</a>.
 */
public class AdminChecker {
  public static final boolean IS_ADMIN = isRunningAsAdministrator();

  private static boolean isRunningAsAdministrator() {
    Preferences systemPrefs = Preferences.systemRoot();
    synchronized (System.err) {
      PrintStream stdErrOriginal = System.err;
      try (PrintStream stdErrMock = new PrintStream(new MockOutputStream())) {
        System.setErr(stdErrMock);
        try {
          systemPrefs.put("foo", "bar"); // SecurityException on Windows
          systemPrefs.remove("foo");
          systemPrefs.flush(); // BackingStoreException on Linux
          return true;
        }
        catch (Exception exception) {
          return false;
        }
      }
      finally {
        System.setErr(stdErrOriginal);
      }
    }
  }

  private static class MockOutputStream extends OutputStream {
    @Override
    public void write(int b) {}
  }
}

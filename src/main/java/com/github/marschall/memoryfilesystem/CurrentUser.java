package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.attribute.UserPrincipal;
import java.util.concurrent.Callable;

public final class CurrentUser {

  private CurrentUser() {
    throw new AssertionError("not instantiable");
  }

  private static final ThreadLocal<UserPrincipal> USER = new ThreadLocal<>();

  public static <V> V useDuring(UserPrincipal user, UserTask<V> task) throws IOException {
    UserPrincipal previous = USER.get();
    try {
      USER.set(user);
      return task.call();
    } finally {
      if (previous == null) {
        USER.remove();
      } else {
        USER.set(previous);
      }
    }
  }

  static UserPrincipal get() {
    return USER.get();
  }

  public static void remove() {
    USER.remove();
  }

  public interface UserTask<V> extends Callable<V> {

    @Override
    public V call() throws IOException;

  }

}

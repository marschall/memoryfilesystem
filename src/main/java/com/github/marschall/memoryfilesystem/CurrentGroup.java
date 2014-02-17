package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.attribute.GroupPrincipal;
import java.util.concurrent.Callable;

public final class CurrentGroup {

  private CurrentGroup() {
    throw new AssertionError("not instantiable");
  }

  private static final ThreadLocal<GroupPrincipal> GROUP = new ThreadLocal<>();

  public static <V> V useDuring(GroupPrincipal group, GroupTask<V> task) throws IOException {
    GroupPrincipal previous = GROUP.get();
    try {
      GROUP.set(group);
      return task.call();
    } finally {
      if (previous == null) {
        GROUP.remove();
      } else {
        GROUP.set(previous);
      }
    }
  }

  static GroupPrincipal get() {
    return GROUP.get();
  }

  public interface GroupTask<V> extends Callable<V> {

    @Override
    public V call() throws IOException;

  }

}

package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.attribute.GroupPrincipal;
import java.util.concurrent.Callable;

/**
 * Provides access to the group of the current user.
 */
public final class CurrentGroup {

  private CurrentGroup() {
    throw new AssertionError("not instantiable");
  }

  private static final ThreadLocal<GroupPrincipal> GROUP = new ThreadLocal<>();

  /**
   * Sets the current group for a certain period.
   *
   * @param group the group to use
   * @param task during this task the given group will be used, will be called
   *  immediately by the current thread
   * @param <V> the type of the return value
   * @return what the task returned
   * @throws IOException if any of the code in the task throws an
   *  {@link IOException}
   */
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

  /**
   * Functional interface for a task during which a certain group should be used.
   *
   * @param <V> the type of the return value
   */
  public interface GroupTask<V> extends Callable<V> {

    /**
     * Executes the task.
     *
     * @return the return value of the task
     * @throws IOException if any of the code in the task throws an
     *  {@link IOException}
     */
    @Override
    V call() throws IOException;

  }

}

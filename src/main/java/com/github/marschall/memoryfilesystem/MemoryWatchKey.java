package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.MemoryWatchKey.State.READY;
import static com.github.marschall.memoryfilesystem.MemoryWatchKey.State.SIGNALLED;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final class MemoryWatchKey implements WatchKey {

  private final AbstractPath path;
  private final Lock lock;
  private boolean isOverflow;
  private State state;
  private boolean valid;
  private final Set<Kind<?>> events;
  private List<WatchEvent<?>> accumulatedEvents;
  private Map<AbstractPath, Integer> accumulatedModificationEvents;
  private final MemoryFileSystemWatchService watcher;

  MemoryWatchKey(AbstractPath path, MemoryFileSystemWatchService watcher, Set<Kind<?>> events) {
    this.path = path;
    this.watcher = watcher;
    this.events = events;
    this.state = READY;
    this.lock = new ReentrantLock();
  }

  boolean accepts(Kind<?> event) {
    return this.events.contains(event);
  }

  @Override
  public List<WatchEvent<?>> pollEvents() {
    try (AutoRelease autoRelease = AutoReleaseLock.autoRelease(this.lock)) {
      if (!this.valid || this.state == SIGNALLED) {
        return Collections.emptyList();
      }

      this.state = SIGNALLED;
      if (this.isOverflow) {
        this.isOverflow = false;
        return Collections.<WatchEvent<?>>singletonList(OverflowWatchEvent.INSTANCE);
      }

      int eventsSize = this.accumulatedEvents.size();
      int modificationSize =  this.accumulatedModificationEvents.size();
      if (addOverflows(eventsSize, modificationSize)) {
        return Collections.<WatchEvent<?>>singletonList(OverflowWatchEvent.INSTANCE);
      }
      int resultSize = eventsSize + modificationSize;
      List<WatchEvent<?>> result = new ArrayList<>(resultSize);
      result.addAll(this.accumulatedEvents);
      for (Entry<AbstractPath, Integer> entry : this.accumulatedModificationEvents.entrySet()) {
        result.add(new ModificationWatchEvent(entry.getKey(), entry.getValue()));
      }
      this.accumulatedEvents.clear();
      this.accumulatedModificationEvents.clear();
      return result;
    }
  }

  private static boolean addOverflows(int x, int y) {
    int r = x + y;
    // HD 2-12 Overflow iff both arguments have the opposite sign of the result
    return ((x ^ r) & (y ^ r)) < 0;
  }

  @Override
  public boolean reset() {
    try (AutoRelease autoRelease = AutoReleaseLock.autoRelease(this.lock)) {
      if (!this.valid) {
        return false;
      }
      this.state = READY;
      return true;
    }
  }

  @Override
  public void cancel() {
    try (AutoRelease autoRelease = AutoReleaseLock.autoRelease(this.lock)) {
      this.valid = false;
      // TODO actually cancel registration
    }
  }

  @Override
  public boolean isValid() {
    try (AutoRelease autoRelease = AutoReleaseLock.autoRelease(this.lock)) {
      return this.valid;
    }
  }

  @Override
  public AbstractPath watchable() {
    return this.path;
  }

  enum State {
    READY,
    SIGNALLED;
  }

  private void fileEvent(AbstractPath eventObject, Kind<Path> kind) {
    if (!this.accepts(kind)) {
      return;
    }
    try (AutoRelease autoRelease = AutoReleaseLock.autoRelease(this.lock)) {
      if (this.isOverflow) {
        return;
      }
      this.accumulatedEvents.add(new KindWatchEvent(eventObject, kind));
      // TODO signal watcher
    }
  }

  void fileCreated(AbstractPath deletedFile) {
    this.fileEvent(deletedFile, ENTRY_CREATE);
  }

  void fileDeleted(AbstractPath newFile) {
    this.fileEvent(newFile, ENTRY_DELETE);
  }

  void fileModified(AbstractPath modifiedFile) {
    if (!this.accepts(ENTRY_MODIFY)) {
      return;
    }
    try (AutoRelease autoRelease = AutoReleaseLock.autoRelease(this.lock)) {
      if (this.isOverflow) {
        return;
      }
      Integer currentCount = this.accumulatedModificationEvents.get(modifiedFile);
      if (currentCount == null) {
        this.accumulatedModificationEvents.put(modifiedFile, 0);
      } else if (currentCount.intValue() == Integer.MAX_VALUE) {
        this.isOverflow = true;
        this.accumulatedEvents.clear();
        this.accumulatedModificationEvents.clear();
      } else {
        this.accumulatedModificationEvents.put(modifiedFile, currentCount + 1);
      }
      // TODO signal watcher
    }
  }

}

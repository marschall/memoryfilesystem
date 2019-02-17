package com.github.marschall.memoryfilesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class MemoryEntryTest {

  private static final String DISPLAY_NAME = "entry: {0}";

  static List<Object[]> data() {
    return Arrays.asList(new Object[][] {
      { new MemoryDirectory("", EntryCreationContextUtil.empty()) },
      { new MemoryFile("", EntryCreationContextUtil.empty()) },
    });
  }

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("data")
  void basicViewName(MemoryEntry memoryEntry) {
    BasicFileAttributeView view = memoryEntry.getBasicFileAttributeView();
    assertEquals("basic", view.name());
  }

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("data")
  void times(MemoryEntry memoryEntry) throws IOException {
    BasicFileAttributeView view = memoryEntry.getBasicFileAttributeView();
    FileTime mTime = FileTime.from(Instant.parse("2004-07-25T18:18:00.111111111Z"));
    FileTime aTime = FileTime.from(Instant.parse("2001-04-21T12:00:00.222222222Z"));
    FileTime cTime = FileTime.from(Instant.parse("1997-08-04T02:04:00.333333333Z"));
    view.setTimes(mTime, aTime, cTime);

    BasicFileAttributes attributes = view.readAttributes();
    assertEquals(cTime, attributes.creationTime());
    assertEquals(mTime, attributes.lastModifiedTime());
    assertEquals(aTime, attributes.lastAccessTime());
  }

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("data")
  void instant(MemoryEntry memoryEntry) throws IOException {
    Instant creationTime = Instant.parse("2019-01-28T01:02:03.046Z");
    Instant lastModificationTime = Instant.parse("2019-01-28T01:02:03.123456Z");
    Instant lastAccessTime = Instant.parse("2019-01-28T01:02:03.123456789Z");

    BasicFileAttributeView view = memoryEntry.getBasicFileAttributeView();
    FileTime mTime = FileTime.from(lastModificationTime);
    FileTime aTime = FileTime.from(lastAccessTime);
    FileTime cTime = FileTime.from(creationTime);
    view.setTimes(mTime, aTime, cTime);

    BasicFileAttributes attributes = view.readAttributes();
    assertEquals(cTime, attributes.creationTime());
    assertEquals(mTime, attributes.lastModifiedTime());
    assertEquals(aTime, attributes.lastAccessTime());

    // check that we keep the granularity (eg: to the nano).
    assertEquals(cTime.toInstant().getNano(), creationTime.getNano());
    assertEquals(mTime.toInstant().getNano(), lastModificationTime.getNano());
    assertEquals(aTime.toInstant().getNano(), lastAccessTime.getNano());
  }
}

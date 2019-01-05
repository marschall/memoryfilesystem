package com.github.marschall.memoryfilesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class MemoryEntryTest {

  private static final String DISPLAY_NAME = "entry: {0}";

  private static final Date M_TIME;
  private static final Date A_TIME;
  private static final Date C_TIME;

  static {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    dateFormat.setLenient(false);

    try {
      M_TIME = dateFormat.parse("2004-07-25 18:18:00 EST");
      A_TIME = dateFormat.parse("2001-04-21 12:00:00 EST");
      C_TIME = dateFormat.parse("1997-08-04 02:04:00 EST");
    } catch (ParseException e) {
      throw new RuntimeException("could not parse date");
    }
  }

  public static List<Object[]> data() {
    return Arrays.asList(new Object[][] {
      { new MemoryDirectory("") },
      { new MemoryFile("", EntryCreationContext.empty()) },
    });
  }

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("data")
  public void basicViewName(MemoryEntry memoryEntry) {
    BasicFileAttributeView view = memoryEntry.getBasicFileAttributeView();
    assertEquals("basic", view.name());
  }

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("data")
  public void times(MemoryEntry memoryEntry) throws IOException {
    BasicFileAttributeView view = memoryEntry.getBasicFileAttributeView();
    FileTime mTime = FileTime.fromMillis(M_TIME.getTime());
    FileTime aTime = FileTime.fromMillis(A_TIME.getTime());
    FileTime cTime = FileTime.fromMillis(C_TIME.getTime());
    view.setTimes(mTime, aTime, cTime);

    BasicFileAttributes attributes = view.readAttributes();
    assertEquals(cTime, attributes.creationTime());
    assertEquals(mTime, attributes.lastModifiedTime());
    assertEquals(aTime, attributes.lastAccessTime());
  }
}

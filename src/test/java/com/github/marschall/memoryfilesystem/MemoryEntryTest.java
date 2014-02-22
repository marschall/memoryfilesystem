package com.github.marschall.memoryfilesystem;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class MemoryEntryTest {

  private static final Date A_TIME;
  private static final Date C_TIME;
  private static final Date M_TIME;

  static {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    dateFormat.setLenient(false);

    try {
      C_TIME = dateFormat.parse("1997-08-04 02:04:00 EST");
      M_TIME = dateFormat.parse("2004-07-25 18:18:00 EST");
      A_TIME = dateFormat.parse("2001-04-21 12:00:00 EST");
    } catch (ParseException e) {
      throw new RuntimeException("could not parse date");
    }
  }

  private final MemoryEntry memoryEntry;

  public MemoryEntryTest(MemoryEntry memoryEntry) {
    this.memoryEntry = memoryEntry;
  }

  @Parameters(name = "entry: {0}")
  public static List<Object[]> data() {
    return Arrays.asList(new Object[][] {
            { new MemoryDirectory("") },
            { new MemoryFile("", EntryCreationContext.empty()) },
    });
  }

  @Test
  public void basicViewName() {
    BasicFileAttributeView view = this.memoryEntry.getBasicFileAttributeView();
    assertEquals("basic", view.name());
  }

  @Test
  public void times() throws IOException {
    BasicFileAttributeView view = this.memoryEntry.getBasicFileAttributeView();
    FileTime cTime = FileTime.fromMillis(C_TIME.getTime());
    FileTime mTime = FileTime.fromMillis(M_TIME.getTime());
    FileTime aTime = FileTime.fromMillis(A_TIME.getTime());
    view.setTimes(mTime, aTime, cTime);

    BasicFileAttributes attributes = view.readAttributes();
    assertEquals(cTime, attributes.creationTime());
    assertEquals(mTime, attributes.lastModifiedTime());
    assertEquals(aTime, attributes.lastAccessTime());
  }
}

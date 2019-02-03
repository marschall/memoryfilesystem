package com.github.marschall.memoryfilesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HundredNanosecondsTest {

  private TemporalUnit unit;

  @BeforeEach
  void setUp() {
    this.unit = new HundredNanoseconds();
  }

  @Test
  void getDuration() {
    Duration unitDur = this.unit.getDuration();

    assertEquals(0L, unitDur.getSeconds());
    assertEquals(100, unitDur.getNano());
  }

  @Test
  void truncate() {
    Instant original = Instant.parse("2019-01-28T01:02:03.123456789Z");
    Instant truncated = Instant.parse("2019-01-28T01:02:03.1234567Z");

    assertEquals(truncated, original.truncatedTo(this.unit));
  }

  @Test
  void plus() {
    OffsetDateTime original = OffsetDateTime.parse("2019-01-28T01:02:03.123456789Z");
    OffsetDateTime added = OffsetDateTime.parse("2019-01-28T01:02:03.123456989Z");

    assertEquals(added, original.plus(2L, this.unit));
  }

}

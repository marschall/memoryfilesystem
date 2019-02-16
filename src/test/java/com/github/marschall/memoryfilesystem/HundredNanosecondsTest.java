package com.github.marschall.memoryfilesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
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
  void durationPlus() {
    Duration duration = Duration.ofMillis(1L).plus(2L, this.unit);

    assertEquals(Duration.ofNanos(1_000_200), duration);
  }

  @Test
  void microSecondRounding() {
    LocalTime inclusive = LocalTime.of(12, 0, 0, 0);
    LocalTime exclusive = LocalTime.of(12, 0, 0, 999);

    assertEquals(0, ChronoUnit.MICROS.between(inclusive, exclusive));
    assertEquals(0, ChronoUnit.MICROS.between(exclusive, inclusive));

    inclusive = LocalTime.of(12, 0, 0, 0);
    exclusive = LocalTime.of(12, 0, 0, 1_001);

    assertEquals(1, ChronoUnit.MICROS.between(inclusive, exclusive));
    assertEquals(-1, ChronoUnit.MICROS.between(exclusive, inclusive));

    inclusive = LocalTime.of(12, 0, 0, 0);
    exclusive = LocalTime.of(12, 0, 0, 1_999);

    assertEquals(1, ChronoUnit.MICROS.between(inclusive, exclusive));
    assertEquals(-1, ChronoUnit.MICROS.between(exclusive, inclusive));
  }

  @Test
  void between() {
    LocalTime inclusive = LocalTime.of(12, 0, 0, 0);
    LocalTime exclusive = LocalTime.of(12, 0, 0, 99);

    assertEquals(0, this.unit.between(inclusive, exclusive));
    assertEquals(0, this.unit.between(exclusive, inclusive));

    inclusive = LocalTime.of(12, 0, 0, 0);
    exclusive = LocalTime.of(12, 0, 0, 101);

    assertEquals(1, this.unit.between(inclusive, exclusive));
    assertEquals(-1, this.unit.between(exclusive, inclusive));

    inclusive = LocalTime.of(12, 0, 0, 0);
    exclusive = LocalTime.of(12, 0, 0, 199);

    assertEquals(1, this.unit.between(inclusive, exclusive));
    assertEquals(-1, this.unit.between(exclusive, inclusive));
  }

  @Test
  void plus() {
    OffsetDateTime original = OffsetDateTime.parse("2019-01-28T01:02:03.123456789Z");
    OffsetDateTime added = OffsetDateTime.parse("2019-01-28T01:02:03.123456989Z");

    assertEquals(added, original.plus(2L, this.unit));
  }

}

package com.github.marschall.memoryfilesystem;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;

/**
 * Represents a unit of 100ns for Windows.
 */
final class HundredNanoseconds implements TemporalUnit {

  private static final Duration DURATION = Duration.ofNanos(100L);

  @Override
  public Duration getDuration() {
    return DURATION;
  }

  @Override
  public boolean isDurationEstimated() {
    return false;
  }

  @Override
  public boolean isDateBased() {
    return false;
  }

  @Override
  public boolean isTimeBased() {
    return true;
  }

  @Override
  public <R extends Temporal> R addTo(R temporal, long amount) {
    return (R) temporal.plus(Math.multiplyExact(amount, 100L), ChronoUnit.NANOS);
  }

  @Override
  public long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive) {
    return ChronoUnit.NANOS.between(temporal1Inclusive, temporal2Exclusive) / 100L;
  }

}

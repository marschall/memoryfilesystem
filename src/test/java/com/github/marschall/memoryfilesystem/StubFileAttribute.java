package com.github.marschall.memoryfilesystem;

import java.nio.file.attribute.FileAttribute;

final class StubFileAttribute<T> implements FileAttribute<T> {

  private final String name;
  private final T value;

  StubFileAttribute(String name, T value) {
    this.name = name;
    this.value = value;
  }

  @Override
  public String name() {
    return this.name;
  }

  @Override
  public T value() {
    return this.value;
  }

  @Override
  public String toString() {
    return this.name + ' ' + this.value;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof StubFileAttribute)) {
      return false;
    }
    StubFileAttribute<?> other = (StubFileAttribute<?>) obj;
    return this.name.equals(other.name)
            && this.value.equals(other.value);
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + this.name.hashCode();
    result = 31 * result + this.value.hashCode();
    return result;
  }

}

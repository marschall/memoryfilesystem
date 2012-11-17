package com.github.marschall.memoryfilesystem;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

final class GlobPathMatcher implements PathMatcher {

  private final Path patternPath;

  GlobPathMatcher(Path patternPath) {
    this.patternPath = patternPath;
    List<Pattern> patterns = new ArrayList<>(patternPath.getNameCount());
    for (int i = 0; i < patternPath.getNameCount(); ++i) {
      patterns.add(this.convertToPattern(((ElementPath) patternPath).getNameElement(i)));
    }
  }

  private Pattern convertToPattern(String element) {
    if (element.equals("**")) {
      // TODO rubber
    }
    Stream stream = new Stream(element);
    StringBuilder buffer = new StringBuilder();

    this.parseGeneric(stream, buffer, ExitHandler.EMPTY, element);
    // TODO Pattern#CANON_EQ ?
    return Pattern.compile(buffer.toString(), Pattern.UNICODE_CASE);
  }

  private char parseGeneric(Stream stream, StringBuilder buffer, ExitHandler exitHandler, String element) {
    while (stream.hasNext()) {
      char next = stream.next();
      if (exitHandler.isExit(next)) {
        return next;
      }
      switch (next) {
        case '*':
          buffer.append(".*");
          break;
        case '?':
          buffer.append('.');
          break;
        case '[':
          this.parseRange(stream, buffer, element);
        case '{':
          this.parseGroup(stream, buffer, element);
        case '\\':
          if (!stream.hasNext()) {
            throw new PatternSyntaxException("\\must be followed by content", element, element.length() - 1);
          }
          buffer.append('\\').append(stream.next());
        default:
      }
    }
    return exitHandler.endOfStream(element);
  }


  private void appendSafe(char c, StringBuilder buffer) {
    if (c == '[' || c == ']' || c == '^' || c == '$' || c == '\\'
            || c == '{' || c == '}'  || c == '.' ) {
      buffer.append('\\');
    }
    buffer.append(c);
  }

  private void parseGroup(Stream stream, StringBuilder buffer, String element) {
    List<String> groups = new ArrayList<>(4);
    StringBuilder groupBuffer = new StringBuilder();

    while (this.parseGeneric(stream, groupBuffer, ExitHandler.GROUP, element) != '}') {
      groups.add(groupBuffer.toString());
      groupBuffer = new StringBuilder(groupBuffer.length());
    }
    groups.add(groupBuffer.toString());

    boolean first = true;
    for (String group : groups) {
      if (!first) {
        buffer.append('|');
        first = false;
      }
      buffer.append('(');
      buffer.append(group);
      buffer.append(')');

    }
    buffer.append(')');

  }

  private void parseRange(Stream stream, StringBuilder buffer, String element) {
    StringBuilder rangeBuffer = new StringBuilder();
    this.parseGeneric(stream, rangeBuffer, ExitHandler.RANGE, element);

    buffer.append('[');
    // TODO escape, think about ignoring -
    buffer.append(rangeBuffer);
    buffer.append(']');
  }

  enum ExitHandler {

    EMPTY {

      @Override
      boolean isExit(char c) {
        return false;
      }

      @Override
      char endOfStream(String element) {
        return 0; // doesn't matter, will be ignored
      }

    },

    GROUP {

      @Override
      boolean isExit(char c) {
        return c == ',' || c == '}';
      }

      @Override
      char endOfStream(String element) {
        throw new PatternSyntaxException("expected }", element, element.length() - 1);
      }

    },

    RANGE {

      @Override
      boolean isExit(char c) {
        return c == ']';
      }

      @Override
      char endOfStream(String element) {
        throw new PatternSyntaxException("expected ]", element, element.length() - 1);
      }

    };

    abstract boolean isExit(char c);

    abstract char endOfStream(String element);

  }

  static final class Stream {

    private final String contents;
    private int position;

    Stream(String contents) {
      this.contents = contents;
      this.position = 0;
    }

    boolean hasNext() {
      return this.position < this.contents.length();
    }

    char next() {
      char value = this.contents.charAt(this.position);
      this.position += 1;
      return value;
    }


  }

  @Override
  public boolean matches(Path path) {
    if (!path.isAbsolute() == this.patternPath.isAbsolute()) {
      return false;
    }
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  static String name() {
    return "glob";
  }

}

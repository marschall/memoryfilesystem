package com.github.marschall.memoryfilesystem;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.UNICODE_CASE;

import java.nio.file.InvalidPathException;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.github.marschall.memoryfilesystem.GlobPathMatcher.GlobPattern;

abstract class PathParser {

  static final String[] EMPTY = new String[0];

  final char separator;
  private final CharacterSet forbiddenCharacters;

  PathParser(String separator, CharacterSet forbiddenCharacters) {
    this.forbiddenCharacters = forbiddenCharacters;
    if (separator.length() != 1) {
      throw new IllegalArgumentException("separator must have length 1 but was \"" + separator + "\"");
    }
    this.separator = separator.charAt(0);
  }

  void check(char c) {
    if (this.forbiddenCharacters.contains(c)) {
      throw new InvalidPathException(Character.toString(c), "contains a not allowed character");
    }
  }

  void check(List<String> elements) {
    for (String element : elements) {
      if (this.forbiddenCharacters.containsAny(element)) {
        throw new InvalidPathException(element, "contains a not allowed character");
      }
    }
  }


  boolean startWithSeparator(String s) {
    char first = s.charAt(0);
    return first == '/' || first == this.separator;
  }

  abstract AbstractPath parse(Map<String, Root> roots, String first, String... more);

  abstract AbstractPath parseUri(Map<String, Root> rootByKey, String uri);

  boolean startWithSeparator(String first, String... more) {
    if (!first.isEmpty()) {
      return this.startWithSeparator(first);
    }
    if (more != null && more.length > 0) {
      for (String s : more) {
        if (!s.isEmpty()) {
          return this.startWithSeparator(s);
        }
      }
    }

    // only empty strings
    return false;
  }


  abstract PathMatcher parseGlob(String pattern);

  abstract boolean isAbsolute(String path);



  PathMatcher compileRegex(String regex, int regexFlags) {
    Pattern pattern = Pattern.compile(regex, regexFlags);
    if (this.isAbsolute(regex)) {
      return new RegexAbsolutePathMatcher(pattern);
    } else {
      return new RegexRelativePathMatcher(pattern);
    }
  }

  PathMatcher transpileGlob(String glob, int regexFlags) {
    StringBuilder regex = new StringBuilder();
    regex.append('^');

    this.transpileGlobInto(new Stream(glob), regex);

    regex.append('$');
    Pattern pattern = Pattern.compile(regex.toString(), regexFlags);
    if (this.isAbsolute(glob)) {
      return new RegexAbsolutePathMatcher(pattern);
    } else {
      return new RegexRelativePathMatcher(pattern);
    }
  }

  private void transpileGlobInto(Stream glob, StringBuilder regex) {
    while (glob.hasNext()) {
      char next = glob.next();
      switch (next) {
        case '*':
          if (glob.hasNext() && glob.peek() == '*') {
            // **
            regex.append(".*");
            glob.next();
          } else {
            // *
            regex.append("[^");
            appendSafe(this.separator, regex);
            regex.append("]*");
          }
          break;
        case '?':
          regex.append("[^");
          appendSafe(this.separator, regex);
          regex.append("]");
          break;
          // [] translates directly
          //        case '[':
          //          break;
          //        case ']':
          //          break;
        case '{':
          regex.append('(');
          String[] subPatterns = glob.upTo('}').split(",");
          for (int i = 0; i < subPatterns.length; i++) {
            String subPattern = subPatterns[i];
            if (i > 0) {
              regex.append('|');
            }
            // TODO should probably disallow nested {
            regex.append('(');
            this.transpileGlobInto(new Stream(subPattern), regex);
            regex.append(')');
          }
          regex.append(')');

          break;
          //        case '}':
          //          parseGroup(stream, buffer, element);
          //          break;
        case '\\':
          if (!glob.hasNext()) {
            throw new PatternSyntaxException("\\must be followed by content", glob.getContents(), glob.getContents().length() - 1);
          }
          regex.append('\\').append(glob.next());
          break;
        default:
          appendSafe(next, regex);
          break;
      }
    }
  }

  static List<GlobPattern> convertToPatterns(List<String> elements) {
    List<GlobPattern> patterns = new ArrayList<>(elements.size());
    for (String element : elements) {
      patterns.add(convertToPattern(element));
    }
    return patterns;
  }

  private static GlobPattern convertToPattern(String element) {
    if (element.equals("**")) {
      return DirectoryCrossingPattern.INSTANCE;
    }
    Stream stream = new Stream(element);
    StringBuilder buffer = new StringBuilder();

    parseGeneric(stream, buffer, ExitHandler.EMPTY, element);
    // TODO Pattern#CANON_EQ ?
    Pattern pattern = Pattern.compile(buffer.toString(), CASE_INSENSITIVE | UNICODE_CASE);
    return new RegexPattern(pattern);
  }

  private static char parseGeneric(Stream stream, StringBuilder buffer, ExitHandler exitHandler, String element) {
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
          parseRange(stream, buffer, element);
          break;
        case '{':
          parseGroup(stream, buffer, element);
          break;
        case '\\':
          if (!stream.hasNext()) {
            throw new PatternSyntaxException("\\must be followed by content", element, element.length() - 1);
          }
          buffer.append('\\').append(stream.next());
          break;
        default:
          appendSafe(next, buffer);
          break;
      }
    }
    return exitHandler.endOfStream(element);
  }

  private static void appendSafe(char c, StringBuilder buffer) {
    if (c == '^' || c == '$' || c == '.'  || c == '\\') {
      buffer.append('\\');
    }
    buffer.append(c);
  }

  private static void parseGroup(Stream stream, StringBuilder buffer, String element) {
    List<String> groups = new ArrayList<>(4);
    StringBuilder groupBuffer = new StringBuilder();

    while (parseGeneric(stream, groupBuffer, ExitHandler.GROUP, element) != '}') {
      groups.add(groupBuffer.toString());
      groupBuffer = new StringBuilder(groupBuffer.length());
    }
    groups.add(groupBuffer.toString());

    boolean first = true;
    buffer.append('(');
    for (String group : groups) {
      if (!first) {
        buffer.append('|');
      } else {
        first = false;
      }
      buffer.append('(');
      buffer.append(group);
      buffer.append(')');
    }
    buffer.append(')');

  }

  private  static void parseRange(Stream stream, StringBuilder buffer, String element) {
    StringBuilder rangeBuffer = new StringBuilder();
    parseGeneric(stream, rangeBuffer, ExitHandler.RANGE, element);

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

    char peek() {
      return this.contents.charAt(this.position);
    }

    String upTo(char delimiter) {
      int start = this.position;
      int index = this.contents.indexOf(delimiter, start);

      while (index != -1 && this.contents.charAt(index - 1) == '\\') {
        start = index + 1;
        index = this.contents.indexOf(delimiter, start);
      }

      if (index == -1) {
        return null;
      } else {
        String substring = this.contents.substring(this.position, index);
        this.position = index + 1;
        return substring;
      }
    }

    String getContents() {
      return this.contents;
    }

  }


  enum DirectoryCrossingPattern implements GlobPattern {

    INSTANCE;

    @Override
    public boolean isCrossingDirectoryBoundaries() {
      return true;
    }

    @Override
    public boolean matches(String element) {
      return true;
    }

    @Override
    public String toString() {
      return "**";
    }

  }

  static final class RegexPattern implements GlobPattern {

    private final Pattern pattern;

    RegexPattern(Pattern pattern) {
      this.pattern = pattern;
    }

    @Override
    public boolean isCrossingDirectoryBoundaries() {
      return false;
    }

    @Override
    public boolean matches(String element) {
      return this.pattern.matcher(element).matches();
    }

    @Override
    public String toString() {
      return this.pattern.toString();
    }

  }

}

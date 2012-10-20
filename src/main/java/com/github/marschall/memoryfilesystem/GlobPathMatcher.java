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
      patterns.add(convertToPattern(((RelativePath) patternPath).getNameElement(i)));
    }
  }
  
  private Pattern convertToPattern(String element) {
    if (element.equals("**")) {
      // TODO rubber
    }
    Stream stream = new Stream(element);
    StringBuilder buffer = new StringBuilder();
    while (stream.hasNext()) {
      char next = stream.next();
      switch (next) {
        case '*':
          buffer.append(".*");
          break;
        case '?':
          buffer.append('.');
          break;
        case '[':
          parseRange(stream, buffer, element);
        case '{':
          parseGroup(stream, buffer, element);
        case '\\':
          if (!stream.hasNext()) {
            throw new PatternSyntaxException("\\must be followed by content", element, element.length() - 1);
          }
          buffer.append('\\').append(stream.next());
        default:
      }
    }
    // TODO Pattern#CANON_EQ ?
    return Pattern.compile(buffer.toString(), Pattern.UNICODE_CASE);
  }
  
  private void parseGroup(Stream stream, StringBuilder buffer, String element) {
    while (stream.hasNext()) {
      char next = stream.next();
      if (next == '}') {
        // TODO build group
      }
      // TODO parse default
    }
    throw new PatternSyntaxException("{ must be closed by }", element, element.length() - 1);
    
  }

  private void parseRange(Stream stream, StringBuilder buffer, String element) {
    while (stream.hasNext()) {
      char next = stream.next();
      if (next == ']') {
        // TODO build rangea
      }
      // TODO parse default
    }
    
    throw new PatternSyntaxException("[ must be closed by ]", element, element.length() - 1);
  }

  private void appendSafe(char c, StringBuilder buffer) {
    //TODO check for regex safe
    buffer.append(c);
  }
  
  static final class Stream {
    
    private final String contents;
    private int position;
    
    Stream(String contents) {
      this.contents = contents;
      this.position = 0;
    }
    
    boolean hasNext() {
      return position < contents.length();
    }
    
    char next() {
      char value = contents.charAt(position);
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

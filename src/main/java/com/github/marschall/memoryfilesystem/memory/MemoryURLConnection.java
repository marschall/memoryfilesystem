package com.github.marschall.memoryfilesystem.memory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.FileNameMap;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.github.marschall.memoryfilesystem.MemoryFileSystemProvider;

final class MemoryURLConnection extends URLConnection {

  private static final String CONTENT_LENGTH = "content-length";
  private static final String CONTENT_TYPE = "content-type";
  private static final String TEXT_PLAIN = "text/plain";
  private static final String LAST_MODIFIED = "last-modified";

  // TODO headers could be UserDefinedFileAttributeView

  private boolean initializedHeaders = false;
  
  private Map<String, List<String>> headerFields;

  private long size;

  private long lastModified;

  private String contentType;

  MemoryURLConnection(URL url) {
    super(url);

    String protocol = url.getProtocol();

    if (!MemoryFileSystemProvider.SCHEME.equals(protocol)) {
      throw new UnsupportedOperationException("Cannot use protocol '"
          + protocol + "' for this implementation");
    }
    
    initializedHeaders = false;
  }

  private void initializeHeaders() {
    if (!this.initializedHeaders) {
      try {
        this.connect();
        BasicFileAttributes attributes = Files.readAttributes(getPath(), BasicFileAttributes.class);
        size = attributes.size();
        lastModified = attributes.lastModifiedTime().toMillis();
        boolean directory = attributes.isDirectory();
        if (directory) {
          this.headerFields = Collections.singletonMap(CONTENT_TYPE, Collections.singletonList(TEXT_PLAIN));
        } else {
          String fileName = getPath().getFileName().toString();
          FileNameMap map = getFileNameMap();
          contentType = map.getContentTypeFor(fileName);
          this.headerFields = computeHeaderFields(size, lastModified, contentType);
        }
      } catch (IOException e) {
        this.headerFields = Collections.emptyMap();
      }
      this.initializedHeaders = true;
    }
  }

  private static Map<String, List<String>> computeHeaderFields(long size, long lastModified, String contentType) {
    Map<String, List<String>> headerFields = new HashMap<>(4);
    if (lastModified != 0) {
      Date date = new Date(lastModified);
      SimpleDateFormat format = new SimpleDateFormat ("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
      format.setTimeZone(TimeZone.getTimeZone("GMT"));
      headerFields.put(LAST_MODIFIED, Collections.singletonList(format.format(date)));
    }
    if (contentType != null) {
      headerFields.put(CONTENT_TYPE, Collections.singletonList(contentType));
    }
    headerFields.put(CONTENT_LENGTH, Collections.singletonList(Long.toString(size)));
    return Collections.unmodifiableMap(headerFields);
  }

  @Override
  public void connect() throws IOException {
    if (!this.connected) {
      if (Files.notExists(this.getPath())) {
        throw new IOException("file does not exist");
      }
      this.connected = true;
    }
  }

  @Override
  public InputStream getInputStream() throws IOException {
    this.connect();
    if (!this.doInput) {
      throw new IllegalStateException("input not supported");
    }
    Path path = this.getPath();
    return Files.newInputStream(path);
  }

  private Path getPath() throws IOException {
    Path path;
    try {
      path = Paths.get(this.url.toURI());
    } catch (URISyntaxException e) {
      throw new IOException("invalid URI syntax", e);
    }
    return path;
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    this.connect();
    if (!this.doOutput) {
      throw new IllegalStateException("input not supported");
    }
    Path path = this.getPath();
    return Files.newOutputStream(path);
  }

  @Override
  public Map<String, List<String>> getHeaderFields() {
    this.initializeHeaders();
    return this.headerFields;
  }

  @Override
  public String getHeaderField(String name) {
    this.initializeHeaders();
    List<String> values = this.headerFields.get(name);
    if (values != null) {
      return values.get(0);
    } else {
      return null;
    }
  }

  @Override
  public long getContentLengthLong() {
    this.initializeHeaders();
    return this.size;
  }

  @Override
  public long getLastModified() {
    this.initializeHeaders();
    return this.lastModified;
  }
  
  @Override
  public String getContentType() {
    this.initializeHeaders();
    return this.contentType;
  }
}

package com.github.marschall.memoryfilesystem.memory;

import com.github.marschall.memoryfilesystem.MemoryFileSystemProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

final class MemoryURLConnection extends URLConnection {

  // TODO check for file existance
  // TODO headers could be UserDefinedFileAttributeView

  MemoryURLConnection(URL url) {
    super(url);

    String protocol = url.getProtocol();

    if (!MemoryFileSystemProvider.SCHEME.equals(protocol)) {
      throw new UnsupportedOperationException("Cannot use protocol '"
          + protocol + "' for this implementation");
    }
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
  public long getContentLengthLong() {
    Path path;
    try {
      path = this.getPath();
      return Files.size(path);
    } catch (IOException e) {
      // javadoc says -1 but file: returns 0
      return 0L;
    }
  }

  @Override
  public long getLastModified() {
    Path path;
    try {
      path = this.getPath();
      return Files.getLastModifiedTime(path).toMillis();
    } catch (IOException e) {
      return 0L;
    }
  }
}

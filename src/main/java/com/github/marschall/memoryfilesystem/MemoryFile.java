package com.github.marschall.memoryfilesystem;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;

class MemoryFile extends MemoryEntry implements MemoryContents {

  /*
   * It turned out to be easier to implement the contents in this class
   * rather than a separate class since it needs access to a, c, m times
   * and update the open count.
   * 
   * #transferTo and #transferFrom are candidates for deadlocks since they
   * acquire two locks without ordering. However one is a read lock
   * and the other is a write lock. So I "think" we're fine for now.
   */

  /**
   * The object header size of an array. Two words (flags &amp; class oop)
   * plus array size (2 *64 bit + 32 bit on 64 bit, 2 *32 bit + 32 bit on 32 bit).
   */
  private static final int ARRAY_HEADER = 8 + 8 + 4;

  static final int BLOCK_SIZE = 4096 - ARRAY_HEADER; //make sure it fits into a 4k memory region

  private final BasicFileAttributes attributes;

  private final InitializingFileAttributeView basicFileAttributeView;

  // lazily allocated, most files probably won't need this
  private LockSet lockSet;

  /**
   * The number of open streams or channels.
   * 
   * <p>A negative number indicates the file is marked for deletion.
   */
  private int openCount;

  /**
   * To store the contents efficiently we store the first {@value #BLOCK_SIZE}
   * bytes in a {@value #BLOCK_SIZE} direct {@code byte[]}. The next
   * {@value #BLOCK_SIZE} * {@value #BLOCK_SIZE} bytes go into a indirect
   * {@code byte[][]} that is lazily allocated.
   */
  private byte[] directBlock;
  private byte[][] indirectBlocks;

  // TODO
  // byte[][][] doubleIndirectBlocks

  private long size;

  private int indirectBlocksAllocated;

  MemoryFile(String originalName, EntryCreationContext context) {
    this(originalName, context, 0);
  }

  MemoryFile(String originalName, EntryCreationContext context, int initialBlocks) {
    super(originalName, context);
    this.attributes = new MemoryFileAttributes();
    this.basicFileAttributeView = new MemoryFileAttributesView();

    if (initialBlocks == 0) {
      // TODO could be made smaller
      this.directBlock = new byte[BLOCK_SIZE];
    } else {
      this.directBlock = new byte[BLOCK_SIZE];
    }
    if (initialBlocks > 1) {
      this.indirectBlocks = new byte[BLOCK_SIZE][];
      for (int i = 0; i < initialBlocks - 1; ++i) {
        this.indirectBlocks[i] = new byte[BLOCK_SIZE];
      }
      this.indirectBlocksAllocated = initialBlocks - 1;
    }
    this.size = 0L;
    this.openCount = 0;
  }

  MemoryFile(String originalName, EntryCreationContext context, MemoryFile other) {
    super(originalName, context);
    this.attributes = new MemoryFileAttributes();
    this.basicFileAttributeView = new MemoryFileAttributesView();

    if (other.directBlock != null) {
      this.directBlock = other.directBlock.clone();
    }

    if (other.indirectBlocks != null) {
      this.indirectBlocks = other.indirectBlocks.clone();
      for (int i = 0; i < other.indirectBlocksAllocated; ++i) {
        this.indirectBlocks[i] = other.indirectBlocks[i].clone();
      }
    }
    this.indirectBlocksAllocated = other.indirectBlocksAllocated;

    this.size = other.size;
    this.openCount = 0;
  }

  @Override
  InitializingFileAttributeView getBasicFileAttributeView() {
    return this.basicFileAttributeView;
  }

  @Override
  BasicFileAttributes getBasicFileAttributes() {
    return this.attributes;
  }


  class MemoryFileAttributesView extends MemoryEntryFileAttributesView {

    @Override
    public BasicFileAttributes readAttributes() throws IOException {
      MemoryFile.this.checkAccess(AccessMode.READ);
      return MemoryFile.this.attributes;
    }

  }

  final class MemoryFileAttributes extends MemoryEntryFileAttributes {

    @Override
    public boolean isRegularFile() {
      return true;
    }

    @Override
    public boolean isDirectory() {
      return false;
    }

    @Override
    public boolean isSymbolicLink() {
      return false;
    }

    @Override
    public boolean isOther() {
      return false;
    }

    @Override
    public long size() {
      return MemoryFile.this.size();
    }

    @Override
    public Object fileKey() {
      // REVIEW think about it
      return MemoryFile.this;
    }

  }

  @Override
  public long size() {
    try (AutoRelease lock = this.readLock()) {
      return this.size;
    }
  }

  InputStream newInputStream(Set<? extends OpenOption> options, Path path) throws IOException {
    boolean deleteOnClose = options.contains(StandardOpenOption.DELETE_ON_CLOSE);
    boolean sync = options.contains(StandardOpenOption.SYNC);
    return this.newInputStream(deleteOnClose, path);
  }

  OutputStream newOutputStream(Set<? extends OpenOption> options, Path path) throws IOException {
    boolean deleteOnClose = options.contains(StandardOpenOption.DELETE_ON_CLOSE);
    boolean append = options.contains(StandardOpenOption.APPEND);
    boolean sync = options.contains(StandardOpenOption.SYNC);
    if (append) {
      return this.newAppendingOutputStream(deleteOnClose, path);
    } else {
      boolean truncate = options.contains(StandardOpenOption.TRUNCATE_EXISTING);
      if (truncate) {
        this.truncate(0L);
      }
      return this.newOutputStream(deleteOnClose, path);
    }
  }

  BlockChannel newChannel(Set<? extends OpenOption> options, Path path) throws IOException {
    boolean append = options.contains(StandardOpenOption.APPEND);
    boolean readable = options.contains(StandardOpenOption.READ);
    boolean deleteOnClose = options.contains(StandardOpenOption.DELETE_ON_CLOSE);
    boolean sync = options.contains(StandardOpenOption.SYNC);
    if (append) {
      return this.newAppendingChannel(readable, deleteOnClose, path);
    } else {
      boolean writable = options.contains(StandardOpenOption.WRITE);
      if (writable) {
        boolean truncate = options.contains(StandardOpenOption.TRUNCATE_EXISTING);
        if (truncate) {
          this.truncate(0L);
        }
      }
      return this.newChannel(readable, writable, deleteOnClose, path);
    }
  }

  InputStream newInputStream(boolean deleteOnClose, Path path) throws IOException {
    try (AutoRelease lock = this.writeLock()) {
      this.incrementOpenCount(path);
      return new BlockInputStream(this, deleteOnClose, path);
    }
  }

  OutputStream newOutputStream(boolean deleteOnClose, Path path) throws IOException {
    try (AutoRelease lock = this.writeLock()) {
      this.incrementOpenCount(path);
      return new NonAppendingBlockOutputStream(this, deleteOnClose, path);
    }
  }

  OutputStream newAppendingOutputStream(boolean deleteOnClose, Path path) throws IOException {
    try (AutoRelease lock = this.writeLock()) {
      this.incrementOpenCount(path);
      return new AppendingBlockOutputStream(this, deleteOnClose, path);
    }
  }

  BlockChannel newChannel(boolean readable, boolean writable, boolean deleteOnClose, Path path) throws IOException {
    try (AutoRelease lock = this.writeLock()) {
      this.incrementOpenCount(path);
      return new NonAppendingBlockChannel(this, readable, writable, deleteOnClose, path);
    }
  }

  BlockChannel newAppendingChannel(boolean readable, boolean deleteOnClose, Path path) throws IOException {
    try (AutoRelease lock = this.writeLock()) {
      this.incrementOpenCount(path);
      return new AppendingBlockChannel(this, readable, this.size, deleteOnClose, path);
    }
  }

  private void incrementOpenCount(Path path) throws NoSuchFileException {
    if (this.openCount < 0) {
      throw new NoSuchFileException(path.toString());
    }
    this.openCount += 1;
  }

  int openCount() {
    return this.openCount;
  }

  void markForDeletion() {
    this.openCount = -1;
  }

  @Override
  public void closedStream(Path toDelete) {
    try (AutoRelease lock = this.writeLock()) {
      this.openCount -= 1;
    }
    if (toDelete != null) {
      // intentionally not covered by Lock
      try {
        Files.delete(toDelete);
      } catch (IOException e) {
        // ignore, only a best effort is made
      }
    }
  }

  @Override
  public void closedChannel(Path toDelete) {
    try (AutoRelease lock = this.writeLock()) {
      this.openCount -= 1;
    }
    if (toDelete != null) {
      // intentionally not covered by Lock
      try {
        Files.delete(toDelete);
      } catch (IOException e) {
        // ignore, only a best effort is made
      }
    }
  }

  private byte[] getBlock(int currentBlock) {
    if (currentBlock == 0) {
      return this.directBlock;
    } else {
      return this.indirectBlocks[currentBlock - 1];
    }
  }

  @Override
  public long read(ByteBuffer dst, long position, long maximum) throws IOException {
    try (AutoRelease lock = this.readLock()) {
      if (position >= this.size) {
        return -1L;
      }
      long remaining = dst.remaining();
      long toRead = min(min(this.size - position, remaining), maximum);
      int currentBlock = (int) (position / BLOCK_SIZE);
      int startIndexInBlock = (int) (position - ((long) currentBlock * (long) BLOCK_SIZE));
      long read = 0L;
      while (read < toRead) {
        int lengthInBlock = (int) min((long) BLOCK_SIZE - (long) startIndexInBlock, toRead - read);

        byte[] block = this.getBlock(currentBlock);
        dst.put(block, startIndexInBlock, lengthInBlock);
        read += lengthInBlock;

        startIndexInBlock = 0;
        currentBlock += 1;
      }
      return read;
    }
  }

  @Override
  public int readShort(ByteBuffer dst, long position) throws IOException {
    return (int) this.read(dst, position, Integer.MAX_VALUE);
  }

  @Override
  public int read(byte[] dst, long position, int off, int len) throws IOException {
    try (AutoRelease lock = this.readLock()) {
      if (position >= this.size) {
        return -1;
      }
      int toRead = (int) min(min(this.size - position, len), Integer.MAX_VALUE);
      int currentBlock = (int) (position / BLOCK_SIZE);
      int startIndexInBlock = (int) (position - ((long) currentBlock * (long) BLOCK_SIZE));
      int read = 0;
      while (read < toRead) {
        int lengthInBlock = (int) min((long) BLOCK_SIZE - (long) startIndexInBlock, (long) toRead - (long) read);

        byte[] block = this.getBlock(currentBlock);
        System.arraycopy(block, startIndexInBlock, dst, off + read, lengthInBlock);
        read += lengthInBlock;

        startIndexInBlock = 0;
        currentBlock += 1;
      }
      return read;
    }
  }

  @Override
  public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
    try (AutoRelease lock = this.writeLock()) {
      this.ensureCapacity(position + count);
      long transferred = 0L;
      long toTransfer = count;

      int currentBlock = (int) (position / BLOCK_SIZE);
      int startIndexInBlock = (int) (position - ((long) currentBlock * (long) BLOCK_SIZE));
      while (transferred < toTransfer) {
        int lengthInBlock = (int) min((long) BLOCK_SIZE - (long) startIndexInBlock, toTransfer - transferred);

        byte[] block = this.getBlock(currentBlock);
        // We can either allocate a new ByteBuffer for every iteration or keep
        // the buffer and copy the contents into it.
        // Since ByteBuffer objects are quite small and don't copy the contents
        // of the backing array allocating a ByteBuffer is probably cheaper.
        ByteBuffer buffer = ByteBuffer.wrap(block, startIndexInBlock, lengthInBlock);
        readFully(buffer, src, lengthInBlock);
        transferred += lengthInBlock;

        startIndexInBlock = 0;
        currentBlock += 1;
      }
      // REVIEW, possibility to fill with random data
      this.size = max(this.size, position + transferred);
      return transferred;
    }
  }

  @Override
  public long transferTo(WritableByteChannel target, long position, long count) throws IOException {
    try (AutoRelease lock = this.readLock()) {
      long transferred = 0L;
      long toTransfer = min(count, this.size - position);

      int currentBlock = (int) (position / BLOCK_SIZE);
      int startIndexInBlock = (int) (position - ((long) currentBlock * (long) BLOCK_SIZE));
      while (transferred < toTransfer) {
        int lengthInBlock = (int) min((long) BLOCK_SIZE - (long) startIndexInBlock, toTransfer - transferred);

        byte[] block = this.getBlock(currentBlock);
        // We can either allocate a new ByteBuffer for every iteration or keep
        // the buffer and copy the contents into it.
        // Since ByteBuffer objects are quite small and don't copy the contents
        // of the backing array allocating a ByteBuffer is probably cheaper.
        ByteBuffer buffer = ByteBuffer.wrap(block, startIndexInBlock, lengthInBlock);
        writeFully(buffer, target, lengthInBlock);
        transferred += lengthInBlock;

        startIndexInBlock = 0;
        currentBlock += 1;
      }

      return transferred;
    }
  }

  private static int writeFully(ByteBuffer src, WritableByteChannel target, int toWrite) throws IOException {
    int written = 0;
    while (written < toWrite) {
      written += target.write(src);
    }
    return written;
  }

  private static int readFully(ByteBuffer src, ReadableByteChannel target, int toRead) throws IOException {
    int read = 0;
    while (read < toRead) {
      read += target.read(src);
    }
    return read;
  }

  @Override
  public long write(ByteBuffer src, long position, long maximum) {
    try (AutoRelease lock = this.writeLock()) {
      long remaining = src.remaining();
      this.ensureCapacity(position + remaining);

      long toWrite = min(remaining, maximum);
      int currentBlock = (int) (position / BLOCK_SIZE);
      int startIndexInBlock = (int) (position - ((long) currentBlock * (long) BLOCK_SIZE));
      long written = 0L;
      while (written < toWrite) {
        int lengthInBlock = (int) min((long) BLOCK_SIZE - (long) startIndexInBlock, toWrite - written);

        byte[] block = this.getBlock(currentBlock);
        src.get(block, startIndexInBlock, lengthInBlock);
        written += lengthInBlock;

        startIndexInBlock = 0;
        currentBlock += 1;
      }
      // REVIEW, possibility to fill with random data
      this.size = max(this.size, position + written);
      return written;
    }
  }

  @Override
  public int writeShort(ByteBuffer src, long position) {
    return (int) this.write(src, position, Integer.MAX_VALUE);
  }

  @Override
  public int write(byte[] src, long position, int off, int len) {
    try (AutoRelease lock = this.writeLock()) {
      this.ensureCapacity(position + len);

      int toWrite = min(len, Integer.MAX_VALUE);
      int currentBlock = (int) (position / BLOCK_SIZE);
      int startIndexInBlock = (int) (position - ((long) currentBlock * (long) BLOCK_SIZE));
      int written = 0;
      while (written < toWrite) {
        int lengthInBlock = (int) min((long) BLOCK_SIZE - (long) startIndexInBlock, (long) toWrite - (long) written);

        byte[] block = this.getBlock(currentBlock);
        System.arraycopy(src, off + written, block, startIndexInBlock, lengthInBlock);
        written += lengthInBlock;

        startIndexInBlock = 0;
        currentBlock += 1;
      }
      // REVIEW, possibility to fill with random data
      this.size = max(this.size, position + written);
      return written;
    }
  }

  @Override
  public long writeAtEnd(ByteBuffer src, long maximum) {
    try (AutoRelease lock = this.writeLock()) {
      return this.write(src, this.size, maximum);
    }
  }

  @Override
  public int writeAtEnd(ByteBuffer src) {
    try (AutoRelease lock = this.writeLock()) {
      return this.writeShort(src, this.size);
    }
  }

  @Override
  public int writeAtEnd(byte[] src, int off, int len) {
    try (AutoRelease lock = this.writeLock()) {
      return this.write(src, this.size, off, len);
    }
  }

  @Override
  public void truncate(long newSize) {
    try (AutoRelease lock = this.writeLock()) {
      if (newSize < this.size) {
        this.size = newSize;
      }
    }
  }

  private void ensureCapacity(long capacity) {
    // if direct block is enough do nothing
    if (capacity <= BLOCK_SIZE) {
      return;
    }

    // lazily allocate indirect blocks
    if (this.indirectBlocks == null) {
      this.indirectBlocks = new byte[BLOCK_SIZE][];
    }

    int blocksRequired = (int) ((capacity - 1L)/ BLOCK_SIZE); // consider already present direct block, don't add + 1

    if (blocksRequired > BLOCK_SIZE) {
      // FIXME implement double indirect addressing
      throw new AssertionError("files bigger than 16GB not yet supported");
    }

    if (blocksRequired > this.indirectBlocksAllocated) {
      for (int i = this.indirectBlocksAllocated; i < blocksRequired; ++i) {
        this.indirectBlocks[i] = new byte[BLOCK_SIZE];
        this.indirectBlocksAllocated += 1;
      }
    }
  }

  @Override
  public void modified() {
    super.modified();
  }

  @Override
  public void accessed() {
    super.accessed();
  }


  @Override
  public MemoryFileLock tryLock(MemoryFileLock lock) {
    try (AutoRelease autoRelease = this.writeLock()) {
      return this.lockSet().tryLock(lock);
    }
  }

  @Override
  public MemoryFileLock lock(MemoryFileLock lock) throws IOException {
    try (AutoRelease autoRelease = this.writeLock()) {
      return this.lockSet().lock(lock);
    }
  }

  @Override
  public void unlock(MemoryFileLock lock) {
    try (AutoRelease autoRelease = this.writeLock()) {
      this.lockSet.remove(lock);
    }
  }

  LockSet lockSet() {
    if (this.lockSet == null) {
      this.lockSet = new LockSet();
    }
    return this.lockSet;
  }


  @Override
  public String toString() {
    return "file(" + this.getOriginalName() + ')';
  }


}

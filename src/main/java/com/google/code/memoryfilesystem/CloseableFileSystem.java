package com.google.code.memoryfilesystem;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Set;

class CloseableFileSystem extends FileSystem {

	private final ClosedFileSystemChecker checker;

	private final MemoryFileSystem delegate;
	
	CloseableFileSystem(ClosedFileSystemChecker checker, MemoryFileSystem delegate) {
		this.delegate = delegate;
		this.checker = checker;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException {
		this.checker.close();
		this.delegate.close();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isOpen() {
		return this.checker.isOpen();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FileSystemProvider provider() {
		this.checker.check();
		return delegate.provider();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isReadOnly() {
		this.checker.check();
		return delegate.isReadOnly();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getSeparator() {
		this.checker.check();
		return delegate.getSeparator();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterable<Path> getRootDirectories() {
		this.checker.check();
		return delegate.getRootDirectories();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterable<FileStore> getFileStores() {
		this.checker.check();
		return delegate.getFileStores();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> supportedFileAttributeViews() {
		this.checker.check();
		return delegate.supportedFileAttributeViews();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Path getPath(String first, String... more) {
		this.checker.check();
		return delegate.getPath(first, more);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PathMatcher getPathMatcher(String syntaxAndPattern) {
		this.checker.check();
		return delegate.getPathMatcher(syntaxAndPattern);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService() {
		this.checker.check();
		return delegate.getUserPrincipalLookupService();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public WatchService newWatchService() throws IOException {
		this.checker.check();
		return delegate.newWatchService();
	}

}

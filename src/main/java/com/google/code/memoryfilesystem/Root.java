package com.google.code.memoryfilesystem;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.Iterator;

abstract class Root extends AbstractPath {

	
	Root(MemoryFileSystem fileSystem) {
		super(fileSystem);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isAbsolute() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Path getRoot() {
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Path getFileName() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Path getParent() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getNameCount() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Path getName(int index) {
		throw new IllegalArgumentException("root does not have any name elements");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Path subpath(int beginIndex, int endIndex) {
		// TODO Auto-generated method stub
		return null;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public Path normalize() {
		return this;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public Path resolve(String other) {
		// TODO Auto-generated method stub
		return null;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public Path resolveSibling(String other) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Path toAbsolutePath() {
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Path toRealPath(boolean resolveLinks) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public WatchKey register(WatchService watcher, Kind<?>[] events,
			Modifier... modifiers) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public WatchKey register(WatchService watcher, Kind<?>... events)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<Path> iterator() {
		return Collections.emptyIterator();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(Path other) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	boolean startsWith(AbstractPath other) {
		return this == other;
	}

	@Override
	boolean endsWith(AbstractPath other) {
		return this == other;
	}

	@Override
	Path resolve(AbstractPath other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	Path resolveSibling(AbstractPath other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	Path relativize(AbstractPath other) {
		// TODO Auto-generated method stub
		return null;
	}

}

package com.google.code.memoryfilesystem;

import static com.google.code.memoryfilesystem.Constants.SAMPLE_ENV;
import static com.google.code.memoryfilesystem.Constants.SAMPLE_URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.junit.Test;

public class MemoryFileSystemTest {
	
	
	@Test
	public void close() throws IOException {
		FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV);
		
		// file system should be open
		assertNotNull(fileSystem);
		assertTrue(fileSystem instanceof MemoryFileSystem);
		assertTrue(fileSystem.isOpen());
		
		// creating a new one should fail
		try {
			FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV);
			fail("file system " + SAMPLE_URI + " already exists");
		} catch (FileSystemAlreadyExistsException e) {
			//should reach here
		}
		
		// closing should work
		fileSystem.close();
		assertFalse(fileSystem.isOpen());
		
		// closing a second time should work
		fileSystem.close();
		assertFalse(fileSystem.isOpen());
		
		// after closing we should be able to create a new one again
		try (FileSystem secondFileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
			assertNotNull(secondFileSystem);
		}
	}
	
	
	@Test
	public void isReadOnly() throws IOException {
		try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
			assertFalse(fileSystem.isReadOnly());
		}
	}
	
	@Test
	public void u() throws IOException {
		FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV);
		try {
			UserPrincipalLookupService userPrincipalLookupService = fileSystem.getUserPrincipalLookupService();
			String userName = System.getProperty("user.name");
			UserPrincipal user = userPrincipalLookupService.lookupPrincipalByName(userName);
			assertEquals(userName, user.getName());
			
			fileSystem.close();
			try {
				userPrincipalLookupService.lookupPrincipalByName(userName);
				fail("UserPrincipalLookupService should be invalid when file system is closed");
			} catch (ClosedFileSystemException e) {
				// should reach here
			}
		} finally {
			fileSystem.close();
		}
	}

	@Test
	public void defaultSeparator() throws IOException {
		try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
			assertEquals("/", fileSystem.getSeparator());
		}
	}
	
	@Test
	public void customSeparator() throws IOException {
		Map<String, Object> env = Collections.singletonMap(MemoryFileSystemProperties.DEFAULT_NAME_SEPARATOR_PROPERTY, (Object) "\\");
		try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, env)) {
			assertEquals("\\", fileSystem.getSeparator());
		}
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void invalidCustomSeparator() throws IOException {
		Map<String, Object> env = Collections.singletonMap(MemoryFileSystemProperties.DEFAULT_NAME_SEPARATOR_PROPERTY, (Object) "\u2603");
		try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, env)) {
			fail("unicode snow man should not be allowed as separator");
		}
	}
	
	@Test
	public void getRootDirectories() throws IOException {
		try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
			Iterator<Path> directories = fileSystem.getRootDirectories().iterator();
			assertTrue(directories.hasNext());
			directories.next();
			try {
				directories.remove();
				fail("root directories iterator should not support remove");
			} catch (UnsupportedOperationException e) {
				// should reach here
			}
			assertFalse(directories.hasNext());
		}
	}

}

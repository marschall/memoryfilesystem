package com.google.code.memoryfilesystem;

import static com.google.code.memoryfilesystem.Constants.SAMPLE_ENV;
import static com.google.code.memoryfilesystem.Constants.SAMPLE_URI;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

class RootRule implements MethodRule {
	
	private Path root;
	

	Path getRoot() {
		return root;
	}



	@Override
	public Statement apply(final Statement base, FrameworkMethod method, Object target) {
		return new Statement() {
			
			@Override
			public void evaluate() throws Throwable {
				try (FileSystem fileSystem = FileSystems.newFileSystem(SAMPLE_URI, SAMPLE_ENV)) {
					root = getRoot(fileSystem);
					base.evaluate();
				}
			}
			
			private Path getRoot(FileSystem fileSystem) {
				return fileSystem.getRootDirectories().iterator().next();
			}
			
		};
	}

}

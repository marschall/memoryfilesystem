package com.google.code.memoryfilesystem;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

public class ZipFileSystemCompability {
	
	@Test
	public void empty() throws URISyntaxException, IOException {
		Path outer = Paths.get("/home/upnip/temp/sample.zip");
		URI uri = new URI("jar:file:/home/upnip/temp/sample.zip");
        Map <String, ?> env = Collections.singletonMap("create", "true");
        Path path = null;
        try (FileSystem fs = FileSystems.newFileSystem(uri, env)) {
        	path = fs.getPath("");
        	System.out.println(path.toUri());
        }
        path.getFileSystem().isReadOnly();
        //outer.endsWith(path);
	}

}

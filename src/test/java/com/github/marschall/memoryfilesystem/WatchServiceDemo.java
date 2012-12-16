package com.github.marschall.memoryfilesystem;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;

public class WatchServiceDemo {

  public static void main(String[] args) throws IOException, InterruptedException {
    Path dir = Paths.get("/Users/marschall/tmp/watch");

    FileSystem fileSystem = dir.getFileSystem();
    try (WatchService service = fileSystem.newWatchService()) {
      WatchKey key = dir.register(service, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);

      while (true) {
        WatchKey taken = service.take();
        List<WatchEvent<?>> events = taken.pollEvents();

        for (WatchEvent<?> event : events) {
          if (event.kind() == ENTRY_CREATE) {
            System.out.println("Created: " + event.context().toString());
          }
          if (event.kind() == ENTRY_DELETE) {
            System.out.println("Delete: " + event.context().toString());
          }
          if (event.kind() == ENTRY_MODIFY) {
            System.out.println("Modify: " + event.context().toString());
          }
        }
        taken.reset();
      }
    }


  }

}

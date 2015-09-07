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
    //    Path dir = Paths.get("/Users/marschall/tmp/watch/file.txt"); -> not a directory java.nio.file.NotDirectoryException
    Path dir = Paths.get("/Users/marschall/tmp/watch/");

    FileSystem fileSystem = dir.getFileSystem();
    try (WatchService service = fileSystem.newWatchService()) {
      WatchKey key = dir.register(service, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);

      while (true) {
        //        WatchKey taken = service.take();
        WatchKey taken = key;
        key.cancel();
        List<WatchEvent<?>> events = taken.pollEvents();

        System.out.println("polled");
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
        final Thread waiter = Thread.currentThread();
        Thread interruptor = new Thread(new Runnable() {

          @Override
          public void run() {
            waiter.interrupt();
          }
        }, "interruptor");
        interruptor.start();
        //        taken.cancel();
      }
    }


  }

}

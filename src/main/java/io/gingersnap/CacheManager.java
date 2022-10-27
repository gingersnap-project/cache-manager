package io.gingersnap;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * @author Ryan Emerson
 */
@Command(name = "cache-manager", mixinStandardHelpOptions = true)
public class CacheManager implements Runnable {
   @Option(names = {"-l", "--lazy-rules"}, description = "The path to a directory containing all LazyCacheRule definitions", required = true)
   Path lazyRulesPath;

   @Override
   public void run() {
      try {
         start();
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private void start() throws InterruptedException, IOException {
      WatchService watchService = FileSystems.getDefault().newWatchService();

      while (!Files.isDirectory(lazyRulesPath)) {
         System.out.printf("'%s' directory does not exist, sleeping ...\n", lazyRulesPath);
         Thread.sleep(1000);
      }

      lazyRulesPath.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

      WatchKey key;
      while ((key = watchService.take()) != null) {
         for (WatchEvent<?> event : key.pollEvents()) {
            @SuppressWarnings("unchecked")
            Path file = lazyRulesPath.resolve(((WatchEvent<Path>) event).context());
            if (Files.isDirectory(file)) {
               System.out.printf("Directory '%s' %s\n", file, event.kind());
               continue;
            }

            WatchEvent.Kind<?> kind = event.kind();
            if (ENTRY_DELETE == kind) {
               System.out.printf("Rule '%s' deleted\n", file);
            } else {
               String rule = Files.readString(file);
               if (ENTRY_CREATE == kind) {
                  System.out.printf("Rule Created:\n%s", rule);
               } else {
                  System.out.printf("Rule Updated:\n%s", rule);
               }
            }
         }
         key.reset();
      }
   }
}

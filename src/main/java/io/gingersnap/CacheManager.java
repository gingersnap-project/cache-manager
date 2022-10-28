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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * @author Ryan Emerson
 */
@Command(name = "cache-manager", mixinStandardHelpOptions = true)
public class CacheManager implements Runnable {
   @Option(names = {"-l", "--lazy-rules"}, description = "The path to a directory containing all LazyCacheRule definitions", required = true)
   File lazyRulesPath;

   @Option(names = {"-e", "--eager-rules"}, description = "The path to a directory containing all EagerCacheRule definitions", required = true)
   File eagerRulesPath;

   @Override
   public void run() {
      try {
         start();
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }
   private void start() throws Exception {
      watching(lazyRulesPath);
      watching(eagerRulesPath);
      // We can't use the Java WatchService api as the inotify implementation is not able to correctly
      // handle drives that are bind mounted in a container, which is the case for ConfigMaps
      // https://blog.arkey.fr/2019/09/13/watchservice-and-bind-mount/
      FileAlterationMonitor monitor = new FileAlterationMonitor(500);
      FileAlterationObserver lazyRulesObserver = new FileAlterationObserver(lazyRulesPath);
      FileAlterationObserver eagerRulesObserver = new FileAlterationObserver(eagerRulesPath);
      lazyRulesObserver.addListener(new RuleListener("LazyCacheRule"));
      eagerRulesObserver.addListener(new RuleListener("EagerCacheRule"));
      monitor.addObserver(lazyRulesObserver);
      monitor.addObserver(eagerRulesObserver);
      monitor.start();
      Thread.currentThread().join();
   }

   private void watching(File path) {
      if (path != null) {
         System.out.printf("Watching '%s'\n", path);
      }
   }

   static class RuleListener extends FileAlterationListenerAdaptor {

      String ruleType;

      RuleListener(String ruleType) {
         this.ruleType = ruleType;
      }

      @Override
      public void onFileCreate(File file) {
         try {
            System.out.printf("%s Created:\n%s", ruleType, Files.readString(file.toPath()));
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      }

      @Override
      public void onFileDelete(File file) {
         System.out.printf("Rule '%s' deleted\n", file);
      }

      @Override
      public void onFileChange(File file) {
         try {
            System.out.printf("%s Updated:\n%s", ruleType, Files.readString(file.toPath()));
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      }

      @Override
      public void onDirectoryCreate(final File directory) {
         System.out.printf("Directory '%s' created\n", directory);
      }

      @Override
      public void onDirectoryDelete(final File directory) {
         System.out.printf("Directory '%s' removed\n", directory);
      }
   }
}

package io.gingersnap;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * @author Ryan Emerson
 */
@Command(name = "cache-manager", mixinStandardHelpOptions = true)
public class CacheManager implements Runnable {
   @Option(names = {"-l", "--lazy-config-map"}, description = "The name of the ConfigMap containing LazyCacheRule definitions", required = true)
   String lazyConfigMap;

   @Inject
   KubernetesClient client;

   @Override
   public void run() {
      try {
         watch();
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private void watch() throws InterruptedException {
      System.out.printf("Watching for %s events\n", lazyConfigMap);

      client.configMaps().withName(lazyConfigMap).watch(new Watcher<>() {
         @Override
         public void eventReceived(Action action, ConfigMap cm) {
            System.out.printf("ConfigMap %s\n", action);
            if (cm.getData() != null) {
               cm.getData().forEach((k, v) -> System.out.printf("%s:\n%s", k, v));
            }
         }

         @Override
         public void onClose(WatcherException e) {
            e.printStackTrace();
         }
      });
      Thread.currentThread().join();
   }
}

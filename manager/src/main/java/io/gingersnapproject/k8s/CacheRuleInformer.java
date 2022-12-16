package io.gingersnapproject.k8s;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.gingersnapproject.k8s.configuration.KubernetesConfiguration;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class CacheRuleInformer {

   private static final Logger log = LoggerFactory.getLogger(CacheRuleInformer.class);

   @Inject
   Instance<KubernetesClient> client;
   @Inject
   KubernetesConfiguration configuration;
   @Inject
   private SharedIndexInformer<ConfigMap> informer;

   void startWatching(@Observes StartupEvent ignore) {
      if (client.isUnsatisfied() || configuration.configMapName().isEmpty()) {
         log.info("Kubernetes client not found, not watching config map");
         return;
      }

      String ruleName = configuration.configMapName().get();
      log.info("Informer on rules in {}", ruleName);
      var RESYNC_PERIOD = 60 * 1000L;
      KubernetesClient kc = client.get();
      informer = kc.configMaps().withName(ruleName).inform(new ConfigMapEventHandler(), RESYNC_PERIOD);
      informer.start();
   }

   public void stop(@Observes ShutdownEvent ignore) {
      if (informer != null) {
         log.info("Shutdown informer");
         informer.close();
      }
   }

}

final class ConfigMapEventHandler implements ResourceEventHandler<ConfigMap> {
   private static final Logger log = LoggerFactory.getLogger(ConfigMapEventHandler.class);

   @Override
   public void onAdd(ConfigMap obj) {
      log.info("Received onAdd({})", obj);
   }

   @Override
   public void onUpdate(ConfigMap oldObj, ConfigMap newObj) {
      log.info("Received onUpdate({}, {})", oldObj, newObj);
   }

   @Override
   public void onDelete(ConfigMap obj, boolean deletedFinalStateUnknown) {
      log.info("Received onDelete({})", obj);
   }
}

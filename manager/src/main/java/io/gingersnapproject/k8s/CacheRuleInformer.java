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
import io.gingersnapproject.configuration.EagerRuleManagement;
import io.gingersnapproject.configuration.LazyRuleManagement;
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
   private SharedIndexInformer<ConfigMap> lazyInformer;
   @Inject
   private SharedIndexInformer<ConfigMap> eagerInformer;
   @Inject
   LazyRuleManagement lrm;
   @Inject
   EagerRuleManagement erm;

   void startWatching(@Observes StartupEvent ignore) {
      if (client.isUnsatisfied() || configuration.lazyConfigMapName().isEmpty()) {
         log.info("Kubernetes client not found, not watching config map");
         return;
      }

      String lazyRuleName = configuration.lazyConfigMapName().get();
      String eagerRuleName = configuration.eagerConfigMapName().get();
      log.info("Informer on lazy rules {} and eager {}", lazyRuleName, eagerRuleName);
      var RESYNC_PERIOD = 60 * 1000L;
      KubernetesClient kc = client.get();
      lazyInformer = kc.configMaps().withName(lazyRuleName).inform(new LazyConfigMapEventHandler(lrm), RESYNC_PERIOD);
      lazyInformer.start();
      eagerInformer = kc.configMaps().withName(eagerRuleName).inform(new EagerConfigMapEventHandler(erm), RESYNC_PERIOD);
      eagerInformer.start();
   }

   public void stop(@Observes ShutdownEvent ignore) {
      if (lazyInformer != null) {
         log.info("Shutdown lazyInformer");
         lazyInformer.close();
      }

      if (eagerInformer != null) {
         log.info("Shutdown eagerInformer");
         eagerInformer.close();
      }
   }

}

final class LazyConfigMapEventHandler implements ResourceEventHandler<ConfigMap> {
   private static final Logger log = LoggerFactory.getLogger(LazyConfigMapEventHandler.class);
   private LazyRuleManagement lrm;

   public LazyConfigMapEventHandler(LazyRuleManagement drm) {
      this.lrm = drm;
   }

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

final class EagerConfigMapEventHandler implements ResourceEventHandler<ConfigMap> {
   private static final Logger log = LoggerFactory.getLogger(LazyConfigMapEventHandler.class);
   private EagerRuleManagement erm;

   public EagerConfigMapEventHandler(EagerRuleManagement erm) {
      this.erm = erm;
   }

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

package io.gingersnap.infinispan.runtime.graal;

import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.counter.impl.CounterModuleLifecycle;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.multimap.impl.MultimapModuleLifecycle;
import org.infinispan.server.core.LifecycleCallbacks;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

public class FixLifecycleCallbacks {
}

@TargetClass(LifecycleCallbacks.class)
final class Target_LifecycleCallbacks {
   @Substitute
   public void cacheManagerStarting(GlobalComponentRegistry gcr, GlobalConfiguration globalConfiguration) {
   }
}

@TargetClass(MultimapModuleLifecycle.class)
final class Target_MultimapModuleLifecycle {
   @Substitute
   public void cacheManagerStarting(GlobalComponentRegistry gcr, GlobalConfiguration globalConfiguration) {
   }
}

@TargetClass(CounterModuleLifecycle.class)
final class Target_CounterModuleLifecycle {
   @Substitute
   public void cacheManagerStarting(GlobalComponentRegistry gcr, GlobalConfiguration globalConfiguration) {
   }
}

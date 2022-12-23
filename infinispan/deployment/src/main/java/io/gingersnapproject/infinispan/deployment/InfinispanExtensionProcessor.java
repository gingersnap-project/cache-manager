package io.gingersnapproject.infinispan.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

class InfinispanExtensionProcessor {
    @BuildStep
    void addReflection(BuildProducer<ReflectiveClassBuildItem> builder) {
        // TODO: add as we need reflection moving forward
        builder.produce(new ReflectiveClassBuildItem(false, false, "com.github.benmanes.caffeine.cache.SSMW"));
        builder.produce(new ReflectiveClassBuildItem(false, false, "com.github.benmanes.caffeine.cache.PSMW"));
    }
}

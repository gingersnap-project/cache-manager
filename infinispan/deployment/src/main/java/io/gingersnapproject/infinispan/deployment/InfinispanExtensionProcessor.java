package io.gingersnapproject.infinispan.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

class InfinispanExtensionProcessor {
    @BuildStep
    void addReflection(BuildProducer<ReflectiveClassBuildItem> reflectionClass) {
        // TODO: add as we need reflection moving forward
    }
}

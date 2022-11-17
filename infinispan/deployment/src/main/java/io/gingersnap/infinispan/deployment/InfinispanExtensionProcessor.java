package io.gingersnap.infinispan.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;

import org.infinispan.commands.module.ModuleCommandExtensions;
import org.infinispan.configuration.parsing.ConfigurationParser;
import org.infinispan.distribution.ch.impl.HashFunctionPartitioner;
import org.infinispan.factories.impl.ModuleMetadataBuilder;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryExpired;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.server.hotrod.HotRodServer;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.gingersnap.infinispan.runtime.EmbeddedCacheManagerProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;

class InfinispanExtensionProcessor {

    @BuildStep
    void addInfinispanDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("org.infinispan", "infinispan-commons"));
        indexDependency.produce(new IndexDependencyBuildItem("org.infinispan", "infinispan-core"));
        indexDependency.produce(new IndexDependencyBuildItem("org.infinispan", "infinispan-server-core"));
        indexDependency.produce(new IndexDependencyBuildItem("org.infinispan", "infinispan-server-hotrod"));
    }

    @BuildStep
    void setup(BuildProducer<FeatureBuildItem> feature,
               BuildProducer<ServiceProviderBuildItem> serviceProvider,
               BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        feature.produce(new FeatureBuildItem("infinispan-extension"));
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(EmbeddedCacheManagerProducer.class));

        addServiceProviders(serviceProvider,
              ConfigurationParser.class,
              ModuleMetadataBuilder.class,
              ModuleCommandExtensions.class
        );
    }

    private void addServiceProviders(BuildProducer<ServiceProviderBuildItem> serviceProvider, Class<?> ...services) {
        for (Class<?> serviceLoadedInterface : services) {
            // Need to register all the modules as service providers so they can be picked up at runtime
            ServiceLoader<?> serviceLoader = ServiceLoader.load(serviceLoadedInterface);
            List<String> interfaceImplementations = new ArrayList<>();
            serviceLoader.forEach(mmb -> interfaceImplementations.add(mmb.getClass().getName()));
            if (!interfaceImplementations.isEmpty()) {
                serviceProvider.produce(new ServiceProviderBuildItem(serviceLoadedInterface.getName(), interfaceImplementations));
            }
        }
    }

    @BuildStep
    void addReflection(BuildProducer<ReflectiveClassBuildItem> reflectionClass, CombinedIndexBuildItem combinedIndexBuildItem) {
        reflectionClass.produce(new ReflectiveClassBuildItem(false, false, HotRodServer.class));
        reflectionClass.produce(new ReflectiveClassBuildItem(false, false, HashFunctionPartitioner.class));
        reflectionClass.produce(new ReflectiveClassBuildItem(false, false,
              "com.github.benmanes.caffeine.cache.SSLMW",
              "com.github.benmanes.caffeine.cache.PSMW",
              "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl",
              "com.sun.org.apache.xerces.internal.jaxp.datatype.DatatypeFactoryImpl",
              "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl",
              "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl",
              "com.sun.xml.bind.v2.ContextFactory",
              "com.sun.xml.internal.bind.v2.ContextFactory",
              "com.sun.xml.internal.stream.XMLInputFactoryImpl"));

        IndexView combinedIndex = combinedIndexBuildItem.getIndex();

        // Add Infinispan listeners to reflection list
        Collection<AnnotationInstance> listenerInstances = combinedIndex.getAnnotations(DotName.createSimple(Listener.class.getName()));
        for (AnnotationInstance instance : listenerInstances) {
            AnnotationTarget target = instance.target();
            if (target.kind() == AnnotationTarget.Kind.CLASS) {
                DotName targetName = target.asClass().name();
                reflectionClass.produce(new ReflectiveClassBuildItem(true, false, targetName.toString()));
            }
        }

        // Handle the various events required by a cluster
        reflectionClass.produce(new ReflectiveClassBuildItem(false, false, CacheEntryCreated.class));
        reflectionClass.produce(new ReflectiveClassBuildItem(false, false, CacheEntryModified.class));
        reflectionClass.produce(new ReflectiveClassBuildItem(false, false, CacheEntryRemoved.class));
    }
}

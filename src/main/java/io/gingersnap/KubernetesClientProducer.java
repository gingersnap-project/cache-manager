package io.gingersnap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

/**
 * @author Ryan Emerson
 */
@Singleton
public class KubernetesClientProducer {

   @Produces
   public KubernetesClient kubernetesClient() {
      try {
         String namespace = Files.readString(Paths.get("/var/run/secrets/kubernetes.io/serviceaccount/namespace"));
         return new DefaultKubernetesClient().inNamespace(namespace);
      } catch (IOException e) {
         System.out.println("Unable to determine local namespace");
         e.printStackTrace();
         System.exit(1);
         return null;
      }
   }
}

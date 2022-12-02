package io.gingersnapproject.search.opensearch.config;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClientBuilder;

import io.quarkus.elasticsearch.restclient.lowlevel.ElasticsearchClientConfig;

@ElasticsearchClientConfig
public class OpenSearchConfig implements RestClientBuilder.HttpClientConfigCallback {

   @Override
   public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
      final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
      credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("admin", "admin"));

      httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
      return httpClientBuilder;
   }
}

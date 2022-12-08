package io.gingersnapproject;

import java.net.InetSocketAddress;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.infinispan.commons.logging.LogFactory;
import org.infinispan.server.core.logging.Log;
import org.infinispan.server.core.transport.NettyTransport;

import io.gingersnapproject.configuration.Configuration;
import io.gingersnapproject.hotrod.CommandProcessor;
import io.gingersnapproject.hotrod.GingersnapDecoder;
import io.gingersnapproject.hotrod.GingersnapServerConfiguration;
import io.gingersnapproject.hotrod.GingersnapServerConfigurationBuilder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@Singleton
public class HotRodServer {
   static private final Log log = LogFactory.getLog(HotRodServer.class, Log.class);

   @Inject
   Configuration config;

   NettyTransport nettyTransport;

   void start(@Observes StartupEvent ignore, Caches maps) {
      GingersnapServerConfiguration configuration = new GingersnapServerConfigurationBuilder(config.hotrod().port())
            .host(config.hotrod().host())
            .build();
      log.infof("Starting Netty transport for %s on %s:%s", configuration.name(), configuration.host(), configuration.port());
      InetSocketAddress address = new InetSocketAddress(configuration.host(), configuration.port());
      nettyTransport = new NettyTransport(address, configuration, "gingersnap", null);

      nettyTransport.initializeHandler(new ChannelInitializer<>() {
         @Override
         protected void initChannel(Channel ch) throws Exception {
            // TODO: look into ssl from NettyChannelInitializer
            ch.pipeline().addLast(new GingersnapDecoder(new CommandProcessor(ch, maps)));
         }
      });

      nettyTransport.start();
   }

   void stop(@Observes ShutdownEvent ignore) {
      if (nettyTransport != null) {
         nettyTransport.stop();
      }
   }

   public boolean isLive() {
      // TODO make more useful
      return nettyTransport.isRunning();
   }

   public boolean isReady() {
      // TODO make more useful
      return nettyTransport.isRunning();
   }

   public boolean hasStarted() {
      // TODO make more useful
      return nettyTransport.isRunning();
   }
}

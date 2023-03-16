/*
 *  Copyright (c) 2023 The original author or authors
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of Apache License v2.0 which
 *  accompanies this distribution.
 *
 *       The Apache License v2.0 is available at
 *       http://www.opensource.org/licenses/apache2.0.php
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.wildfly.extension.vertx;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.VertxBuilder;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.cluster.infinispan.InfinispanClusterManager;
import org.infinispan.configuration.global.TransportConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.jboss.as.server.ServerEnvironment;
import org.jgroups.JChannel;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;

import java.nio.file.Paths;
import java.util.UUID;
import java.util.function.Supplier;

import static org.wildfly.extension.vertx.VertxConstants.DEFAULT_INFINISPAN_FILE;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
class ClusterVertxHolder {

  private static final ClusterVertxHolder INSTANCE = new ClusterVertxHolder();
  private ClusterVertxHolder() {}

  static ClusterVertxHolder getInstance() {
    return INSTANCE;
  }

  private volatile DefaultCacheManager defaultCacheManager;

  Vertx clusterVertx(VertxBuilder vb, Supplier<ChannelFactory> channelFactorySupplier, Supplier<String> clusterSupplier,
                      String jgroupsStackFile, VertxOptions vertxOptions, Supplier<ServerEnvironment> serverEnvSupplier,
                      VertxProxy vertxProxy) throws Exception {
    ClassLoader classLoader = getClass().getClassLoader();
    ConfigurationBuilderHolder builderHolder = new ParserRegistry(classLoader).parseFile(DEFAULT_INFINISPAN_FILE);
    TransportConfigurationBuilder transport = builderHolder.getGlobalConfigurationBuilder().transport();
    if (channelFactorySupplier != null) {
      JChannel jChannel = channelFactorySupplier.get().createChannel(UUID.randomUUID().toString());
      String clusterName = clusterSupplier.get() != null ? clusterSupplier.get() : vertxProxy.getJgroupChannelName();
      transport.defaultTransport().removeProperty(JGroupsTransport.CHANNEL_CONFIGURATOR)
        .transport(new JGroupsTransport(jChannel))
        .clusterName(clusterName);
    } else if (jgroupsStackFile != null) {
      String jgroupsFile = Paths.get(jgroupsStackFile).isAbsolute() ? jgroupsStackFile :
        Paths.get(serverEnvSupplier.get().getServerConfigurationDir().getPath(), jgroupsStackFile).toString();
      transport.defaultTransport().removeProperty(JGroupsTransport.CHANNEL_CONFIGURATOR)
        .addProperty(JGroupsTransport.CONFIGURATION_FILE, jgroupsFile);
    }
    defaultCacheManager = new DefaultCacheManager(builderHolder, true);
    ClusterManager clusterManager = new InfinispanClusterManager(defaultCacheManager);
    vertxOptions.setClusterManager(clusterManager);
    vb.clusterManager(clusterManager);
    Promise<Vertx> promise = Promise.promise();
    vb.init().clusteredVertx(promise);
    return promise.future().toCompletionStage().toCompletableFuture().get();
  }

  Future<Void> stopClusterVertx() {
    try {
      if (defaultCacheManager != null) {
        defaultCacheManager.stop();
        defaultCacheManager = null;
      }
    } catch (Exception e) {
      return Future.failedFuture(e);
    }
    return Future.succeededFuture();
  }

}

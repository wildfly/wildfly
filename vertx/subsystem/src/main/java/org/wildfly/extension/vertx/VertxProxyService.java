/*
 * Copyright (C) 2020 RedHat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.jboss.as.controller.OperationContext;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.msc.Service;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jgroups.JChannel;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;

import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.wildfly.extension.vertx.logging.VertxLogger.VERTX_LOGGER;

/**
 * The msc service to initialize a Vertx instance.
 *
 * @author <a href="aoingl@gmail.com">Lin Gao</a>
 */
public class VertxProxyService implements Service, VertxConstants {
    private final VertxProxy vertxProxy;
    private final Supplier<ChannelFactory> channelFactorySupplier;
    private final Supplier<String> clusterSupplier;
    private final Supplier<NamedVertxOptions> optionsSupplier;
    private final Supplier<ServerEnvironment> serverEnvSupplier;
    private final String jgroupsStackFile;
    final Consumer<VertxProxy> vertxProxytConsumer;
    private volatile Vertx vertx;
    private volatile DefaultCacheManager defaultCacheManager;

    static void installService(OperationContext context, VertxProxy vertxProxy, String optionName) {
        VertxProxyService vertxProxyService;
        ServiceName vertxServiceName = VertxResourceDefinition.VERTX_RUNTIME_CAPABILITY.getCapabilityServiceName(vertxProxy.getName());
        ServiceBuilder<?> vertxServiceBuilder = context.getServiceTarget().addService(vertxServiceName);
        Supplier<ServerEnvironment> serverEnvSupplier = vertxServiceBuilder.requires(ServerEnvironmentService.SERVICE_NAME);
        final Consumer<VertxProxy> vertxProxytConsumer = vertxServiceBuilder.provides(vertxServiceName);
        Supplier<NamedVertxOptions> optionsSupplier;
        if (optionName == null) {
            optionsSupplier = () -> NamedVertxOptions.DEFAULT;
        } else {
            ServiceName optionServiceName = AbstractVertxOptionsResourceDefinition.VERTX_OPTIONS_CAPABILITY.getCapabilityServiceName(optionName);
            optionsSupplier = vertxServiceBuilder.requires(optionServiceName);
        }
        if (vertxProxy.isClustered()) {
            final String jgroupChannel = vertxProxy.getJgroupChannelName();
            final String jgroupsStackFile = vertxProxy.getJgroupsStackFile();
            // channel factory can be either from source or forked.
            // new channel and transport ports need to be specified if multiple Vertx instances are to be created.
            // cluster name cannot be overridden.
            Supplier<ChannelFactory> cacheManagerSupplier = null;
            if (jgroupChannel != null) {
                final ServiceName channelFactoryServiceName;
                if (vertxProxy.isForkedChannel()) {
                    channelFactoryServiceName = JGroupsRequirement.CHANNEL_FACTORY.getServiceName(context, jgroupChannel);
                } else {
                    channelFactoryServiceName = JGroupsRequirement.CHANNEL_SOURCE.getServiceName(context, jgroupChannel);
                }
                cacheManagerSupplier = vertxServiceBuilder.requires(channelFactoryServiceName);
            }
            ServiceName clusterServiceName = JGroupsRequirement.CHANNEL_CLUSTER.getServiceName(context, jgroupChannel);
            Supplier<String> clusterSupplier = vertxServiceBuilder.requires(clusterServiceName);
            vertxProxyService = new VertxProxyService(cacheManagerSupplier, clusterSupplier, vertxProxy, optionsSupplier, vertxProxytConsumer, serverEnvSupplier, jgroupsStackFile);
        } else  {
            vertxProxyService = new VertxProxyService(null, null, vertxProxy, optionsSupplier, vertxProxytConsumer, serverEnvSupplier, null);
        }
        vertxServiceBuilder.setInstance(vertxProxyService);
        vertxServiceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
        vertxServiceBuilder.install();

        final String jndiName = vertxProxy.getJndiName();
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);
        final BinderService binderService = new BinderService(bindInfo.getBindName());
        VertxManagementReferenceFactory valueManagedReferenceFactory = new VertxManagementReferenceFactory(vertxProxyService);
        binderService.getManagedObjectInjector().inject(valueManagedReferenceFactory);
        final ServiceBuilder<?> builder = context.getServiceTarget().addService(bindInfo.getBinderServiceName());
        builder.addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector());
        builder.addDependency(vertxServiceName);
        builder.setInstance(binderService)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .addListener(new LifecycleListener() {
                    private volatile boolean bound;
                    @Override
                    public void handleEvent(ServiceController<?> controller, LifecycleEvent event) {
                        switch (event) {
                            case UP: {
                                VERTX_LOGGER.vertxStarted(vertxProxy.getName(), jndiName);
                                bound = true;
                                break;
                            }
                            case DOWN: {
                                if (bound) {
                                    VERTX_LOGGER.vertxStopped(vertxProxy.getName(), jndiName);
                                    bound = false;
                                }
                                break;
                            }
                            case REMOVED: {
                                VERTX_LOGGER.vertxRemoved(vertxProxy.getName(), jndiName);
                                break;
                            }
                        }
                    }
                })
                .install();
    }

    Vertx getValue() {
        return this.vertx;
    }

    private VertxProxyService(Supplier<ChannelFactory> channelFactorySupplier, Supplier<String> clusterSupplier,
                              VertxProxy vertxProxy, Supplier<NamedVertxOptions> optionsSupplier,
                              Consumer<VertxProxy> vertxProxytConsumer,
                              Supplier<ServerEnvironment> serverEnvSupplier,
                              String jgroupsStackFile) {
        this.vertxProxy = vertxProxy;
        this.clusterSupplier = clusterSupplier;
        this.channelFactorySupplier = channelFactorySupplier;
        this.optionsSupplier = optionsSupplier;
        this.vertxProxytConsumer = vertxProxytConsumer;
        this.serverEnvSupplier = serverEnvSupplier;
        this.jgroupsStackFile = jgroupsStackFile;
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            this.vertx = createVertx();
            vertxProxy.setVertx(this.vertx);
        } catch (Exception e) {
            throw VERTX_LOGGER.failedToStartVertxService(vertxProxy.getName(), e);
        }
        VertxRegistry.INSTANCE.registerVertx(vertxProxy);
        vertxProxytConsumer.accept(vertxProxy);
    }

    private Vertx createVertx() throws Exception {
        VertxOptions vertxOptions = optionsSupplier.get().getVertxOptions();
        if (vertxOptions == null) {
            vertxOptions = new VertxOptions();
        }
        VertxBuilder vb = new VertxBuilder(vertxOptions);
        if (vertxProxy.isClustered()) {
            ClassLoader classLoader = getClass().getClassLoader();
            ConfigurationBuilderHolder builderHolder = new ParserRegistry(classLoader)
              .parseFile(DEFAULT_INFINISPAN_FILE);
            TransportConfigurationBuilder transport = builderHolder.getGlobalConfigurationBuilder()
              .transport();
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
        } else {
            return vb.init().vertx();
        }
    }

    @Override
    public void stop(StopContext context) {
        CompletableFuture<Void> closeFuture = (CompletableFuture<Void>)this.vertx.close()
                .flatMap(v -> {
                    try {
                        if (defaultCacheManager != null) {
                            defaultCacheManager.stop();
                        }
                    } catch (Exception e) {
                        return Future.failedFuture(e);
                    }
                    return Future.<Void>succeededFuture();
                })
                .toCompletionStage();
        try {
            closeFuture.get();
        } catch (Exception e) {
            VERTX_LOGGER.errorWhenClosingVertx(vertxProxy.getName(), e);
        } finally {
            VertxRegistry.INSTANCE.unRegister(vertxProxy.getName());
            this.vertx = null;
            vertxProxy.setVertx(null);
        }
    }

}

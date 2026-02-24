/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.client;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.RemoteSchemasAdmin;
import org.infinispan.client.hotrod.RemoteSchemasAdmin.SchemaOpResult;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.RemoteCacheConfiguration;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.SerializationContextInitializer;
import org.jboss.as.clustering.infinispan.logging.InfinispanLogger;
import org.jboss.as.clustering.infinispan.marshalling.UserMarshallerFactory;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.wildfly.clustering.cache.infinispan.remote.RemoteCacheContainerDecorator;
import org.wildfly.clustering.function.Runner;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.server.Registrar;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Container managed {@link RemoteCacheManager} decorator, whose lifecycle methods are no-ops.
 *
 * @author Radoslav Husar
 * @author Paul Ferraro
 */
public class ManagedRemoteCacheContainer extends RemoteCacheContainerDecorator implements RemoteCacheContainer {

    private final RemoteCacheManager container;
    private final String name;
    private final ModuleLoader loader;
    private final Registrar<String> registrar;

    public ManagedRemoteCacheContainer(RemoteCacheManager container, String name, ModuleLoader loader, Registrar<String> registrar) {
        super(container);
        this.container = container;
        this.name = name;
        this.loader = loader;
        this.registrar = registrar;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public <K, V> RemoteCache<K, V> getCache(String cacheName) {
        List<Runnable> stopTasks = new LinkedList<>();
        ClassLoader loader = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        Module module = Module.forClassLoader(loader, false);
        // If thread context is that of a deployment ...
        if ((module != null) && module.getName().startsWith(ServiceModuleLoader.MODULE_PREFIX)) {
            Configuration configuration = this.container.getConfiguration();
            Map<String, RemoteCacheConfiguration> configurations = configuration.remoteCaches();
            synchronized (configurations) {
                // If remote cache configuration is undefined, auto-create
                if (!configurations.containsKey(cacheName)) {
                    Marshaller marshaller = UserMarshallerFactory.forMediaType(this.container.getMarshaller().mediaType()).createUserMarshaller(this.loader, List.of(loader));
                    // If this is a protostream marshaller, additionally auto-register deployment-specific schemas with server
                    if (marshaller.mediaType().equals(MediaType.APPLICATION_PROTOSTREAM)) {
                        RemoteSchemasAdmin admin = this.container.administration().schemas();
                        for (SerializationContextInitializer initializer : ServiceLoader.load(SerializationContextInitializer.class, loader)) {
                            if (initializer instanceof GeneratedSchema schema) {
                                if (WildFlySecurityManager.getClassLoaderPrivileged(schema.getClass()) == loader) {
                                    SchemaOpResult result = admin.create(schema);
                                    if (result.hasError()) {
                                        InfinispanLogger.ROOT_LOGGER.warn(result.getError());
                                    } else {
                                        stopTasks.add(() -> admin.remove(schema.getName()));
                                    }
                                }
                            }
                        }
                    }
                    configuration.addRemoteCache(cacheName, builder -> builder.marshaller(marshaller));
                    stopTasks.add(() -> configuration.removeRemoteCache(cacheName));
                }
            }
        }

        RemoteCache<K, V> cache = this.container.getCache(cacheName);
        return (cache != null) ? new ManagedRemoteCache<>(this, this.container, cache, this.registrar) {
            @Override
            public void stop() {
                super.stop();
                Runner.of(stopTasks);
            }
        }: null;
    }

    @Override
    public void start() {
        // Disable lifecycle methods
    }

    @Override
    public void stop() {
        // Disable lifecycle methods
    }

    @Override
    public String[] getServers() {
        return this.container.getServers();
    }

    @Override
    public int getActiveConnectionCount() {
        return this.container.getActiveConnectionCount();
    }

    @Override
    public int getConnectionCount() {
        return this.container.getConnectionCount();
    }

    @Override
    public int getIdleConnectionCount() {
        return this.container.getIdleConnectionCount();
    }

    @Override
    public long getRetries() {
        return this.container.getRetries();
    }

    @Override
    public String toString() {
        return this.name;
    }
}

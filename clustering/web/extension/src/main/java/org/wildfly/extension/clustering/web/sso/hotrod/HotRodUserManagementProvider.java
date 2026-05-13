/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.sso.hotrod;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.infinispan.client.hotrod.DataFormat;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
import org.infinispan.client.hotrod.configuration.RemoteCacheConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.TransactionMode;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.marshall.Marshaller;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.modules.Module;
import org.wildfly.clustering.cache.infinispan.marshalling.MediaTypes;
import org.wildfly.clustering.cache.infinispan.marshalling.UserMarshaller;
import org.wildfly.clustering.cache.infinispan.remote.RemoteCacheConfiguration;
import org.wildfly.clustering.cache.infinispan.remote.transaction.RemoteTransactionManagerLookup;
import org.wildfly.clustering.infinispan.client.service.HotRodServiceDescriptor;
import org.wildfly.clustering.infinispan.client.service.RemoteCacheConfigurationServiceInstallerFactory;
import org.wildfly.clustering.infinispan.client.service.RemoteCacheServiceInstallerFactory;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContextBuilder;
import org.wildfly.clustering.marshalling.protostream.modules.ModuleClassLoaderMarshaller;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.session.infinispan.remote.user.HotRodUserManagerFactory;
import org.wildfly.clustering.session.user.UserManagerFactory;
import org.wildfly.clustering.web.service.user.DistributableUserManagementProvider;
import org.wildfly.security.cache.CachedIdentity;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class HotRodUserManagementProvider implements DistributableUserManagementProvider {
    private static final String DEFAULT_CONFIGURATION = """
{
    "distributed-cache": {
        "encoding" : {
            "key" : {
                "media-type" : "application/octet-stream"
            },
            "value" : {
                "media-type" : "application/octet-stream"
            }
        },
        "locking" : {
            "isolation" : "REPEATABLE_READ"
        },
        "mode" : "SYNC",
        "transaction" : {
            "mode" : "NON_XA",
            "locking" : "PESSIMISTIC"
        }
    }
}""";

    private final BinaryServiceConfiguration configuration;

    public HotRodUserManagementProvider(BinaryServiceConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Iterable<ServiceInstaller> getServiceInstallers(String name) {
        String templateName = this.configuration.getChildName();
        Module module = Module.forClass(HotRodUserManagerFactory.class);
        Marshaller marshaller = new UserMarshaller(MediaTypes.WILDFLY_PROTOSTREAM, new ProtoStreamByteBufferMarshaller(SerializationContextBuilder.newInstance(new ModuleClassLoaderMarshaller(module)).load(module.getClassLoader()).build()));

        Consumer<RemoteCacheConfigurationBuilder> configurator = new Consumer<>() {
            @Override
            public void accept(RemoteCacheConfigurationBuilder builder) {
                // Near caching not compatible with max-idle expiration.
                builder.forceReturnValues(false).marshaller(marshaller).nearCacheMode(NearCacheMode.DISABLED).transactionMode(TransactionMode.NON_XA).transactionManagerLookup(RemoteTransactionManagerLookup.INSTANCE);
                if (templateName != null) {
                    builder.templateName(templateName);
                } else {
                    builder.configuration(DEFAULT_CONFIGURATION);
                }
            }
        };
        BinaryServiceConfiguration configuration = this.configuration.withChildName(name);
        ServiceInstaller configurationInstaller = new RemoteCacheConfigurationServiceInstallerFactory(configurator).apply(configuration);
        ServiceInstaller cacheInstaller = RemoteCacheServiceInstallerFactory.INSTANCE.apply(configuration);

        DataFormat format = DataFormat.builder().keyType(MediaType.APPLICATION_OCTET_STREAM).keyMarshaller(marshaller).valueType(MediaType.APPLICATION_OCTET_STREAM).valueMarshaller(marshaller).build();
        ServiceDependency<UserManagerFactory<CachedIdentity, String, Map.Entry<String, URI>>> factory = configuration.getServiceDependency(HotRodServiceDescriptor.REMOTE_CACHE).map(new Function<>() {
            @Override
            public UserManagerFactory<CachedIdentity, String, Map.Entry<String, URI>> apply(RemoteCache<?, ?> cache) {
                RemoteCacheConfiguration remoteCacheConfiguration = new RemoteCacheConfiguration() {
                    @Override
                    public <K, V> RemoteCache<K, V> getCache() {
                        return cache.withDataFormat(format);
                    }
                };
                return new HotRodUserManagerFactory<>(remoteCacheConfiguration);
            }
        });
        ServiceInstaller installer = ServiceInstaller.BlockingBuilder.of(factory)
                .provides(ServiceNameFactory.resolveServiceName(DistributableUserManagementProvider.USER_MANAGER_FACTORY, name))
                .build();
        return List.of(configurationInstaller, cacheInstaller, installer);
    }
}

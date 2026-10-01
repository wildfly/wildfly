/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.session.hotrod;

import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.servlet.ServletContext;

import org.infinispan.client.hotrod.DataFormat;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
import org.infinispan.client.hotrod.configuration.RemoteCacheConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.TransactionMode;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.marshall.Marshaller;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.modules.Module;
import org.wildfly.clustering.cache.infinispan.marshalling.MediaTypes;
import org.wildfly.clustering.cache.infinispan.marshalling.UserMarshaller;
import org.wildfly.clustering.cache.infinispan.remote.RemoteCacheConfiguration;
import org.wildfly.clustering.cache.infinispan.remote.transaction.RemoteTransactionManagerLookup;
import org.wildfly.clustering.infinispan.client.service.HotRodServiceDescriptor;
import org.wildfly.clustering.infinispan.client.service.RemoteCacheConfigurationServiceInstallerFactory;
import org.wildfly.clustering.infinispan.client.service.RemoteCacheServiceInstallerFactory;
import org.wildfly.clustering.marshalling.protostream.ImmutableSerializationContext;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamConfiguration;
import org.wildfly.clustering.marshalling.protostream.modules.ModuleClassResolver;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.session.SessionManagerFactory;
import org.wildfly.clustering.session.infinispan.remote.HotRodSessionManagerFactory;
import org.wildfly.clustering.web.service.deployment.WebDeploymentServiceDescriptor;
import org.wildfly.clustering.web.service.routing.RouteLocatorProvider;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementConfiguration;
import org.wildfly.clustering.web.service.session.SessionManagerFactoryConfiguration;
import org.wildfly.extension.clustering.web.session.AbstractSessionManagementProvider;
import org.wildfly.service.BlockingLifecycle;
import org.wildfly.subsystem.service.DeploymentServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class HotRodSessionManagementProvider extends AbstractSessionManagementProvider {
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

    public HotRodSessionManagementProvider(DistributableSessionManagementConfiguration<DeploymentUnit> configuration, BinaryServiceConfiguration cacheConfiguration, RouteLocatorProvider locatorProvider) {
        super(configuration, cacheConfiguration, locatorProvider);
    }

    @Override
    public <C> DeploymentServiceInstaller getSessionManagerFactoryServiceInstaller(SessionManagerFactoryConfiguration<C> configuration) {
        BinaryServiceConfiguration deploymentCacheConfiguration = this.getCacheConfiguration().withChildName(configuration.getDeploymentName());
        String templateName = this.getCacheConfiguration().getChildName();
        Module module = Module.forClass(HotRodSessionManagerFactory.class);
        ProtoStreamConfiguration config = ProtoStreamConfiguration.Builder.with(new ModuleClassResolver(module)).build();
        ImmutableSerializationContext context = ImmutableSerializationContext.Builder.with(config).build();
        Marshaller marshaller = new UserMarshaller(MediaTypes.WILDFLY_PROTOSTREAM, new ProtoStreamByteBufferMarshaller(context));

        Consumer<RemoteCacheConfigurationBuilder> configurator = new Consumer<>() {
            @Override
            public void accept(RemoteCacheConfigurationBuilder builder) {
                builder.forceReturnValues(false).marshaller(marshaller).nearCacheMode(NearCacheMode.DISABLED).transactionMode(TransactionMode.NON_XA).transactionManagerLookup(RemoteTransactionManagerLookup.INSTANCE);
                if (templateName != null) {
                    builder.templateName(templateName);
                } else {
                    builder.configuration(DEFAULT_CONFIGURATION);
                }
            }
        };
        DeploymentServiceInstaller configurationInstaller = new RemoteCacheConfigurationServiceInstallerFactory(configurator).apply(deploymentCacheConfiguration);
        DeploymentServiceInstaller cacheInstaller = RemoteCacheServiceInstallerFactory.INSTANCE.apply(deploymentCacheConfiguration);

        DataFormat format = DataFormat.builder().keyType(MediaType.APPLICATION_OCTET_STREAM).keyMarshaller(marshaller).valueType(MediaType.APPLICATION_OCTET_STREAM).valueMarshaller(marshaller).build();
        ServiceDependency<SessionManagerFactory<ServletContext, C>> factory = deploymentCacheConfiguration.getServiceDependency(HotRodServiceDescriptor.REMOTE_CACHE).map(new Function<>() {
            @Override
            public SessionManagerFactory<ServletContext, C> apply(RemoteCache<?, ?> cache) {
                RemoteCacheConfiguration config = new RemoteCacheConfiguration() {
                    @Override
                    public <CK, CV> RemoteCache<CK, CV> getCache() {
                        return cache.withDataFormat(format);
                    }
                };
                return new HotRodSessionManagerFactory<>(new HotRodSessionManagerFactory.Configuration<>() {
                    @Override
                    public SessionManagerFactoryConfiguration<C> getSessionManagerFactoryConfiguration() {
                        return configuration;
                    }

                    @Override
                    public RemoteCacheConfiguration getCacheConfiguration() {
                        return config;
                    }
                });
            }
        });
        DeploymentServiceInstaller installer = ServiceInstaller.BlockingBuilder.of(factory)
                .provides(WebDeploymentServiceDescriptor.SESSION_MANAGER_FACTORY.resolve(configuration.getDeploymentUnit()))
                .withLifecycle(BlockingLifecycle.autoClose())
                .build();

        return DeploymentServiceInstaller.combine(configurationInstaller, cacheInstaller, installer);
    }
}

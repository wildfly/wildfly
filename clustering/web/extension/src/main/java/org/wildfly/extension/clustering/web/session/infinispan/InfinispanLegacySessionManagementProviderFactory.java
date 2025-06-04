/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.session.infinispan;

import java.util.function.Function;

import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.metadata.web.jboss.ReplicationConfig;
import org.jboss.metadata.web.jboss.ReplicationGranularity;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceName;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.jboss.JBossByteBufferMarshaller;
import org.wildfly.clustering.marshalling.jboss.MarshallingConfigurationBuilder;
import org.wildfly.clustering.marshalling.jboss.MarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.jboss.externalizer.LegacyExternalizerConfiguratorFactory;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.session.SessionAttributePersistenceStrategy;
import org.wildfly.clustering.web.service.deployment.WebDeploymentConfiguration;
import org.wildfly.clustering.web.service.routing.RouteLocatorProvider;
import org.wildfly.clustering.web.service.routing.RoutingProvider;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementConfiguration;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;
import org.wildfly.clustering.web.service.session.LegacyDistributableSessionManagementProviderFactory;
import org.wildfly.extension.clustering.web.routing.LocalRouteLocatorProvider;
import org.wildfly.extension.clustering.web.routing.infinispan.PrimaryOwnerRouteLocatorProvider;
import org.wildfly.subsystem.service.DeploymentServiceInstaller;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(LegacyDistributableSessionManagementProviderFactory.class)
@Deprecated
public class InfinispanLegacySessionManagementProviderFactory implements LegacyDistributableSessionManagementProviderFactory, Function<DeploymentUnit, ByteBufferMarshaller> {

    private enum JBossMarshallingVersion implements Function<Module, MarshallingConfiguration> {

        VERSION_1() {
            @Override
            public MarshallingConfiguration apply(Module module) {
                return MarshallingConfigurationBuilder.newInstance(ModularClassResolver.getInstance(module.getModuleLoader())).build();
            }
        },
        VERSION_2() {
            @Override
            public MarshallingConfiguration apply(Module module) {
                MarshallingConfigurationBuilder builder = MarshallingConfigurationBuilder.newInstance(ModularClassResolver.getInstance(module.getModuleLoader()));
                return new LegacyExternalizerConfiguratorFactory(module.getClassLoader()).apply(builder).build();
            }
        },
        ;
        static final JBossMarshallingVersion CURRENT = VERSION_2;
    }

    @Override
    public DistributableSessionManagementProvider createSessionManagerProvider(DeploymentUnit unit, ReplicationConfig config) {
        // Determine container and cache names using legacy logic
        String replicationConfigCacheName = (config != null) ? config.getCacheName() : null;
        ServiceName replicationConfigServiceName = ServiceNameFactory.parseServiceName((replicationConfigCacheName != null) ? replicationConfigCacheName : "web");
        ServiceName baseReplicationConfigServiceName = ServiceName.JBOSS.append("infinispan");
        if (!baseReplicationConfigServiceName.isParentOf(replicationConfigServiceName)) {
            replicationConfigServiceName = baseReplicationConfigServiceName.append(replicationConfigServiceName);
        }
        String containerName = ((replicationConfigServiceName.length() > 3) ? replicationConfigServiceName.getParent() : replicationConfigServiceName).getSimpleName();
        String cacheName = (replicationConfigServiceName.length() > 3) ? replicationConfigServiceName.getSimpleName() : null;
        DistributableSessionManagementConfiguration<DeploymentUnit> configuration = new DistributableSessionManagementConfiguration<>() {
            @Override
            public SessionAttributePersistenceStrategy getAttributePersistenceStrategy() {
                ReplicationGranularity granularity = (config != null) ? config.getReplicationGranularity() : null;
                return (granularity == ReplicationGranularity.ATTRIBUTE) ? SessionAttributePersistenceStrategy.FINE : SessionAttributePersistenceStrategy.COARSE;
            }

            @Override
            public Function<DeploymentUnit, ByteBufferMarshaller> getMarshallerFactory() {
                // Legacy session management was hard-coded to use JBoss Marshalling
                return InfinispanLegacySessionManagementProviderFactory.this;
            }
        };
        return new InfinispanSessionManagementProvider(configuration, BinaryServiceConfiguration.of(containerName, cacheName), new RouteLocatorProvider() {
            @Override
            public DeploymentServiceInstaller getServiceInstaller(BinaryServiceConfiguration infinispan, WebDeploymentConfiguration deployment) {
                CapabilityServiceSupport support = unit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
                // Legacy session management was hard-coded to use primary owner routing
                // Detect case where distributable-web subsystem exists, but configuration is not compatible with legacy deployment and thus local routing is required
                boolean forceLocalRouting = support.hasCapability(RoutingProvider.SERVICE_DESCRIPTOR) && !support.hasCapability(RoutingProvider.INFINISPAN_SERVICE_DESCRIPTOR);
                RouteLocatorProvider provider = forceLocalRouting ? new LocalRouteLocatorProvider() : new PrimaryOwnerRouteLocatorProvider();
                return provider.getServiceInstaller(infinispan, deployment);
            }
        });
    }

    @Override
    public ByteBufferMarshaller apply(DeploymentUnit unit) {
        Module module = unit.getAttachment(Attachments.MODULE);
        return new JBossByteBufferMarshaller(MarshallingConfigurationRepository.from(JBossMarshallingVersion.CURRENT, module), module.getClassLoader());
    }
}

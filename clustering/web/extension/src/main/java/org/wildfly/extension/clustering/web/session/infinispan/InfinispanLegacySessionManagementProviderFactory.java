/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.clustering.web.session.infinispan;

import java.io.Externalizable;
import java.io.Serializable;
import java.util.function.Function;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
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
import org.wildfly.clustering.marshalling.jboss.DynamicExternalizerObjectTable;
import org.wildfly.clustering.marshalling.jboss.JBossByteBufferMarshaller;
import org.wildfly.clustering.marshalling.jboss.SimpleClassTable;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.web.WebDeploymentConfiguration;
import org.wildfly.clustering.web.infinispan.session.InfinispanSessionManagementConfiguration;
import org.wildfly.clustering.web.service.WebRequirement;
import org.wildfly.clustering.web.service.routing.RouteLocatorServiceConfiguratorFactory;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;
import org.wildfly.clustering.web.service.session.LegacySessionManagementProviderFactory;
import org.wildfly.clustering.web.session.SessionAttributePersistenceStrategy;
import org.wildfly.extension.clustering.web.routing.LocalRouteLocatorServiceConfigurator;
import org.wildfly.extension.clustering.web.routing.infinispan.PrimaryOwnerRouteLocatorServiceConfigurator;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(LegacySessionManagementProviderFactory.class)
@Deprecated
public class InfinispanLegacySessionManagementProviderFactory implements LegacySessionManagementProviderFactory<InfinispanSessionManagementConfiguration<DeploymentUnit>>, Function<DeploymentUnit, ByteBufferMarshaller> {

    private enum JBossMarshallingVersion implements Function<Module, MarshallingConfiguration> {

        VERSION_1() {
            @Override
            public MarshallingConfiguration apply(Module module) {
                MarshallingConfiguration config = new MarshallingConfiguration();
                config.setClassResolver(ModularClassResolver.getInstance(module.getModuleLoader()));
                config.setClassTable(new SimpleClassTable(Serializable.class, Externalizable.class));
                return config;
            }
        },
        VERSION_2() {
            @Override
            public MarshallingConfiguration apply(Module module) {
                MarshallingConfiguration config = new MarshallingConfiguration();
                config.setClassResolver(ModularClassResolver.getInstance(module.getModuleLoader()));
                config.setClassTable(new SimpleClassTable(Serializable.class, Externalizable.class));
                config.setObjectTable(new DynamicExternalizerObjectTable(module.getClassLoader()));
                return config;
            }
        },
        ;
        static final JBossMarshallingVersion CURRENT = VERSION_2;
    }

    @Override
    public DistributableSessionManagementProvider<InfinispanSessionManagementConfiguration<DeploymentUnit>> createSessionManagerProvider(DeploymentUnit unit, ReplicationConfig config) {
        // Determine container and cache names using legacy logic
        String replicationConfigCacheName = (config != null) ? config.getCacheName() : null;
        ServiceName replicationConfigServiceName = ServiceNameFactory.parseServiceName((replicationConfigCacheName != null) ? replicationConfigCacheName : "web");
        ServiceName baseReplicationConfigServiceName = ServiceName.JBOSS.append("infinispan");
        if (!baseReplicationConfigServiceName.isParentOf(replicationConfigServiceName)) {
            replicationConfigServiceName = baseReplicationConfigServiceName.append(replicationConfigServiceName);
        }
        String containerName = ((replicationConfigServiceName.length() > 3) ? replicationConfigServiceName.getParent() : replicationConfigServiceName).getSimpleName();
        String cacheName = (replicationConfigServiceName.length() > 3) ? replicationConfigServiceName.getSimpleName() : null;
        InfinispanSessionManagementConfiguration<DeploymentUnit> configuration = new InfinispanSessionManagementConfiguration<>() {
            @Override
            public String getContainerName() {
                return containerName;
            }

            @Override
            public String getCacheName() {
                return cacheName;
            }

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
        return new InfinispanSessionManagementProvider(configuration, new LegacyRouteLocatorServiceConfiguratorFactory(unit));
    }

    @Override
    public ByteBufferMarshaller apply(DeploymentUnit unit) {
        Module module = unit.getAttachment(Attachments.MODULE);
        return new JBossByteBufferMarshaller(new SimpleMarshallingConfigurationRepository(JBossMarshallingVersion.class, JBossMarshallingVersion.CURRENT, module), module.getClassLoader());
    }

    private static class LegacyRouteLocatorServiceConfiguratorFactory implements RouteLocatorServiceConfiguratorFactory<InfinispanSessionManagementConfiguration<DeploymentUnit>> {
        private final DeploymentUnit unit;

        LegacyRouteLocatorServiceConfiguratorFactory(DeploymentUnit unit) {
            this.unit = unit;
        }

        @Override
        public CapabilityServiceConfigurator createRouteLocatorServiceConfigurator(InfinispanSessionManagementConfiguration<DeploymentUnit> configuration, WebDeploymentConfiguration deploymentConfiguration) {
            CapabilityServiceSupport support = this.unit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
            // Legacy session management was hard-coded to use primary owner routing
            // Detect case where distributable-web subsystem exists, but configuration is not compatible with legacy deployment and thus local routing is required
            boolean forceLocalRouting = support.hasCapability(WebRequirement.ROUTING_PROVIDER.getName()) && !support.hasCapability(WebRequirement.INFINISPAN_ROUTING_PROVIDER.getName());
            return forceLocalRouting ? new LocalRouteLocatorServiceConfigurator(deploymentConfiguration) : new PrimaryOwnerRouteLocatorServiceConfigurator(configuration, deploymentConfiguration);
        }
    }
}

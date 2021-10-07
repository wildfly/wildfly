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

package org.wildfly.clustering.web.infinispan.session;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.metadata.web.jboss.ReplicationConfig;
import org.jboss.metadata.web.jboss.ReplicationGranularity;
import org.jboss.msc.service.ServiceName;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.web.WebDeploymentConfiguration;
import org.wildfly.clustering.web.infinispan.routing.PrimaryOwnerRouteLocatorServiceConfigurator;
import org.wildfly.clustering.web.routing.RouteLocatorServiceConfiguratorFactory;
import org.wildfly.clustering.web.session.DistributableSessionManagementProvider;
import org.wildfly.clustering.web.session.LegacySessionManagementProviderFactory;
import org.wildfly.clustering.web.session.SessionAttributePersistenceStrategy;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(LegacySessionManagementProviderFactory.class)
@Deprecated
public class InfinispanLegacySessionManagementProviderFactory implements LegacySessionManagementProviderFactory, RouteLocatorServiceConfiguratorFactory<InfinispanSessionManagementConfiguration> {

    @Override
    public DistributableSessionManagementProvider createSessionManagerProvider(ReplicationConfig config) {
        // Determine container and cache names using legacy logic
        String replicationConfigCacheName = (config != null) ? config.getCacheName() : null;
        ServiceName replicationConfigServiceName = ServiceNameFactory.parseServiceName((replicationConfigCacheName != null) ? replicationConfigCacheName : "web");
        ServiceName baseReplicationConfigServiceName = ServiceName.JBOSS.append("infinispan");
        if (!baseReplicationConfigServiceName.isParentOf(replicationConfigServiceName)) {
            replicationConfigServiceName = baseReplicationConfigServiceName.append(replicationConfigServiceName);
        }
        String containerName = ((replicationConfigServiceName.length() > 3) ? replicationConfigServiceName.getParent() : replicationConfigServiceName).getSimpleName();
        String cacheName = (replicationConfigServiceName.length() > 3) ? replicationConfigServiceName.getSimpleName() : null;
        InfinispanSessionManagementConfiguration configuration = new InfinispanSessionManagementConfiguration() {
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
        };
        return new InfinispanSessionManagementProvider(configuration, this);
    }

    @Override
    public CapabilityServiceConfigurator createRouteLocatorServiceConfigurator(InfinispanSessionManagementConfiguration managementConfiguration, WebDeploymentConfiguration deploymentConfiguration) {
        // Legacy session management was hard-coded to use primary owner routing
        return new PrimaryOwnerRouteLocatorServiceConfigurator(managementConfiguration, deploymentConfiguration);
    }
}

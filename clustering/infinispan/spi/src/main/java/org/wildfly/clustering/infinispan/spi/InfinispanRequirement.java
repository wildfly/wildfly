/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.spi;

import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.jboss.as.clustering.controller.UnaryRequirementServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryServiceNameFactoryProvider;
import org.wildfly.clustering.infinispan.spi.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.service.UnaryRequirement;

/**
 * @author Paul Ferraro
 */
public enum InfinispanRequirement implements UnaryRequirement, UnaryServiceNameFactoryProvider {

    CONTAINER("org.wildfly.clustering.infinispan.cache-container", CacheContainer.class),
    CONFIGURATION("org.wildfly.clustering.infinispan.cache-container-configuration", GlobalConfiguration.class),
    KEY_AFFINITY_FACTORY("org.wildfly.clustering.infinispan.key-affinity-factory", KeyAffinityServiceFactory.class),
    REMOTE_CONTAINER("org.wildfly.clustering.infinispan.remote-cache-container", RemoteCacheContainer.class),
    REMOTE_CONTAINER_CONFIGURATION("org.wildfly.clustering.infinispan.remote-cache-container-configuration", Configuration.class),
    ;
    private final String name;
    private final Class<?> type;
    private final UnaryServiceNameFactory factory = new UnaryRequirementServiceNameFactory(this);

    InfinispanRequirement(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Class<?> getType() {
        return this.type;
    }

    @Override
    public UnaryServiceNameFactory getServiceNameFactory() {
        return this.factory;
    }
}

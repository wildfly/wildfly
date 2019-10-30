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
package org.wildfly.clustering.spi;

import org.jboss.as.clustering.controller.BinaryRequirementServiceNameFactory;
import org.jboss.as.clustering.controller.BinaryServiceNameFactory;
import org.jboss.as.clustering.controller.DefaultableBinaryServiceNameFactoryProvider;
import org.jboss.as.clustering.controller.UnaryServiceNameFactory;
import org.wildfly.clustering.service.BinaryRequirement;
import org.wildfly.clustering.service.DefaultableBinaryRequirement;
import org.wildfly.clustering.service.UnaryRequirement;
import org.wildfly.clustering.singleton.SingletonCacheRequirement;

/**
 * @author Paul Ferraro
 */
public enum ClusteringCacheRequirement implements DefaultableBinaryRequirement, DefaultableBinaryServiceNameFactoryProvider {
    GROUP("org.wildfly.clustering.cache.group", ClusteringDefaultCacheRequirement.GROUP),
    @Deprecated NODE_FACTORY("org.wildfly.clustering.cache.node-factory", ClusteringDefaultCacheRequirement.NODE_FACTORY),
    REGISTRY("org.wildfly.clustering.cache.registry", ClusteringDefaultCacheRequirement.REGISTRY),
    REGISTRY_ENTRY("org.wildfly.clustering.cache.registry-entry", ClusteringDefaultCacheRequirement.REGISTRY_ENTRY),
    REGISTRY_FACTORY("org.wildfly.clustering.cache.registry-factory", ClusteringDefaultCacheRequirement.REGISTRY_FACTORY),
    SERVICE_PROVIDER_REGISTRY("org.wildfly.clustering.cache.service-provider-registry", ClusteringDefaultCacheRequirement.SERVICE_PROVIDER_REGISTRY),
    @Deprecated SINGLETON_SERVICE_BUILDER_FACTORY(SingletonCacheRequirement.SINGLETON_SERVICE_BUILDER_FACTORY, ClusteringDefaultCacheRequirement.SINGLETON_SERVICE_BUILDER_FACTORY),
    SINGLETON_SERVICE_CONFIGURATOR_FACTORY(SingletonCacheRequirement.SINGLETON_SERVICE_CONFIGURATOR_FACTORY, ClusteringDefaultCacheRequirement.SINGLETON_SERVICE_CONFIGURATOR_FACTORY),
    ;
    private final String name;
    private final BinaryServiceNameFactory factory = new BinaryRequirementServiceNameFactory(this);
    private final ClusteringDefaultCacheRequirement defaultRequirement;

    ClusteringCacheRequirement(BinaryRequirement requirement, ClusteringDefaultCacheRequirement defaultRequirement) {
        this(requirement.getName(), defaultRequirement);
    }

    ClusteringCacheRequirement(String name, ClusteringDefaultCacheRequirement defaultRequirement) {
        this.name = name;
        this.defaultRequirement = defaultRequirement;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public UnaryRequirement getDefaultRequirement() {
        return this.defaultRequirement;
    }

    @Override
    public BinaryServiceNameFactory getServiceNameFactory() {
        return this.factory;
    }

    @Override
    public UnaryServiceNameFactory getDefaultServiceNameFactory() {
        return this.defaultRequirement;
    }
}

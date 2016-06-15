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

package org.wildfly.clustering.server;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.BiFunction;

import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.clustering.naming.BinderServiceBuilder;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.spi.CacheBuilderProvider;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;
import org.wildfly.clustering.spi.ServiceNameRegistry;

/**
 * @author Paul Ferraro
 */
public class CacheRequirementBuilderProvider<T> implements CacheBuilderProvider {

    private final ClusteringCacheRequirement requirement;
    private final CacheCapabilityServiceBuilderFactory<T> factory;
    private final BiFunction<String, String, JndiName> jndiNameFactory;

    protected CacheRequirementBuilderProvider(ClusteringCacheRequirement requirement, CacheCapabilityServiceBuilderFactory<T> factory) {
        this(requirement, factory, null);
    }

    protected CacheRequirementBuilderProvider(ClusteringCacheRequirement requirement, CacheCapabilityServiceBuilderFactory<T> factory, BiFunction<String, String, JndiName> jndiNameFactory) {
        this.requirement = requirement;
        this.factory = factory;
        this.jndiNameFactory = jndiNameFactory;
    }

    @Override
    public Collection<CapabilityServiceBuilder<?>> getBuilders(ServiceNameRegistry<ClusteringCacheRequirement> registry, String containerName, String cacheName) {
        ServiceName name = registry.getServiceName(this.requirement);
        CapabilityServiceBuilder<?> builder = this.factory.createBuilder(name, containerName, cacheName);
        if (this.jndiNameFactory == null) {
            return Collections.singleton(builder);
        }
        ContextNames.BindInfo binding = ContextNames.bindInfoFor(this.jndiNameFactory.apply(containerName, cacheName).getAbsoluteName());
        CapabilityServiceBuilder<?> binderBuilder = new BinderServiceBuilder<>(binding, builder.getServiceName(), this.requirement.getType());
        return Arrays.asList(builder, binderBuilder);
    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }
}

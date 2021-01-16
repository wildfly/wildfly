/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.cache.simple;

import java.util.Collections;
import java.util.function.Consumer;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ejb3.cache.CacheFactoryBuilder;
import org.jboss.as.ejb3.cache.CacheFactoryBuilderServiceNameProvider;
import org.jboss.as.ejb3.cache.Identifiable;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * Service that provides a simple {@link CacheFactoryBuilder}.
 *
 * @author Paul Ferraro
 *
 * @param <K> the cache key type
 * @param <V> the cache value type
 */
public class SimpleCacheFactoryBuilderServiceConfigurator<K, V extends Identifiable<K>> extends CacheFactoryBuilderServiceNameProvider implements ServiceConfigurator, CacheFactoryBuilder<K, V> {

    public SimpleCacheFactoryBuilderServiceConfigurator(String name) {
        super(name);
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<CacheFactoryBuilder<K, V>> cacheFactoryBuilder = builder.provides(name);
        Service service = Service.newInstance(cacheFactoryBuilder, this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public Iterable<CapabilityServiceConfigurator> getDeploymentServiceConfigurators(DeploymentUnit unit) {
        return Collections.emptySet();
    }

    @Override
    public CapabilityServiceConfigurator getServiceConfigurator(ServiceName name, StatefulComponentDescription description, ComponentConfiguration configuration) {
        return new SimpleCacheFactoryServiceConfigurator<>(name, description);
    }

    @Override
    public boolean supportsPassivation() {
        return false;
    }
}

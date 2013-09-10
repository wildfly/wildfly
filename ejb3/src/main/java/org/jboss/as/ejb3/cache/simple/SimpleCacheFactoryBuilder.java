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

import org.jboss.as.ejb3.cache.CacheFactory;
import org.jboss.as.ejb3.cache.CacheFactoryBuilder;
import org.jboss.as.ejb3.cache.Identifiable;
import org.jboss.as.ejb3.component.stateful.StatefulTimeoutInfo;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.ejb.BeanContext;

/**
 * Builds a service that provides a simple {@link CacheFactory}.
 *
 * @author Paul Ferraro
 *
 * @param <K>
 * @param <V>
 */
public class SimpleCacheFactoryBuilder<K, V extends Identifiable<K>> implements CacheFactoryBuilder<K, V> {

    @Override
    public void installDeploymentUnitDependencies(ServiceTarget target, ServiceName deploymentUnitServiceName) {
        // Nothing to install
    }

    @Override
    public ServiceBuilder<? extends CacheFactory<K, V>> build(ServiceTarget target, ServiceName name, BeanContext context, StatefulTimeoutInfo timeout) {
        InjectedValue<ServerEnvironment> environment = new InjectedValue<>();
        return target.addService(name, new SimpleCacheFactoryService<K, V>(environment, timeout))
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, environment)
        ;
    }

    @Override
    public boolean supportsPassivation() {
        return false;
    }
}

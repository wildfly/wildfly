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

import java.util.concurrent.ScheduledExecutorService;

import org.jboss.as.ejb3.cache.Cache;
import org.jboss.as.ejb3.cache.CacheFactory;
import org.jboss.as.ejb3.cache.Identifiable;
import org.jboss.as.ejb3.cache.StatefulObjectFactory;
import org.jboss.as.ejb3.component.stateful.StatefulTimeoutInfo;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.ejb.BeanContext;
import org.wildfly.clustering.ejb.IdentifierFactory;
import org.wildfly.clustering.ejb.PassivationListener;

/**
 * Service that provides a simple {@link CacheFactory}.
 *
 * @author Paul Ferraro
 *
 * @param <K> the cache key type
 * @param <V> the cache value type
 */
public class SimpleCacheFactoryService<K, V extends Identifiable<K>> extends AbstractService<CacheFactory<K, V>> implements CacheFactory<K, V> {

    public static <K, V extends Identifiable<K>> ServiceBuilder<CacheFactory<K, V>> build(String name, ServiceTarget target, ServiceName serviceName, BeanContext context, StatefulTimeoutInfo timeout) {
        SimpleCacheFactoryService<K, V> service = new SimpleCacheFactoryService<>(timeout);
        return target.addService(serviceName, service)
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, service.environment)
                .addDependency(context.getDeploymentUnitServiceName().append(name, "expiration"), ScheduledExecutorService.class, service.executor)
        ;
    }

    private final InjectedValue<ServerEnvironment> environment = new InjectedValue<>();
    private final InjectedValue<ScheduledExecutorService> executor = new InjectedValue<>();
    private final StatefulTimeoutInfo timeout;

    private SimpleCacheFactoryService(StatefulTimeoutInfo timeout) {
        this.timeout = timeout;
    }

    @Override
    public CacheFactory<K, V> getValue() {
        return this;
    }

    @Override
    public Cache<K, V> createCache(IdentifierFactory<K> identifierFactory, StatefulObjectFactory<V> factory, PassivationListener<V> passivationListener) {
        return new SimpleCache<>(factory, identifierFactory, this.timeout, this.environment.getValue(), this.executor.getValue());
    }
}

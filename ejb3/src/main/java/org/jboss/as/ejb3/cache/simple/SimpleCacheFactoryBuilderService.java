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

import java.security.AccessController;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.clustering.concurrent.RemoveOnCancelScheduledExecutorService;
import org.jboss.as.ejb3.cache.CacheFactory;
import org.jboss.as.ejb3.cache.CacheFactoryBuilder;
import org.jboss.as.ejb3.cache.CacheFactoryBuilderService;
import org.jboss.as.ejb3.cache.Identifiable;
import org.jboss.as.ejb3.component.stateful.StatefulTimeoutInfo;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.threads.JBossThreadFactory;
import org.wildfly.clustering.ejb.BeanContext;
import org.wildfly.security.manager.action.GetAccessControlContextAction;

/**
 * Service that provides a simple {@link CacheFactoryBuilder}.
 *
 * @author Paul Ferraro
 *
 * @param <K> the cache key type
 * @param <V> the cache value type
 */
public class SimpleCacheFactoryBuilderService<K, V extends Identifiable<K>> extends CacheFactoryBuilderService<K, V> implements CacheFactoryBuilder<K, V>  {

    private static final ThreadFactory THREAD_FACTORY = new JBossThreadFactory(new ThreadGroup(SimpleCache.class.getSimpleName()), Boolean.FALSE, null, "%G - %t", null, null, AccessController.doPrivileged(GetAccessControlContextAction.getInstance()));

    private final String name;

    public SimpleCacheFactoryBuilderService(String name) {
        super(name);
        this.name = name;
    }

    @Override
    public CacheFactoryBuilder<K, V> getValue() {
        return this;
    }

    @Override
    public void installDeploymentUnitDependencies(ServiceTarget target, ServiceName deploymentUnitServiceName) {
        target.addService(deploymentUnitServiceName.append(this.name, "expiration"), new RemoveOnCancelScheduledExecutorService(THREAD_FACTORY))
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install()
        ;
    }

    @Override
    public ServiceBuilder<? extends CacheFactory<K, V>> build(ServiceTarget target, ServiceName name, BeanContext context, StatefulTimeoutInfo timeout) {
        return SimpleCacheFactoryService.build(this.name, target, name, context, timeout);
    }

    @Override
    public boolean supportsPassivation() {
        return false;
    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.jpa.hibernate3.infinispan;

import java.util.EnumSet;
import java.util.Properties;

import org.hibernate.cache.CacheException;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.infinispan.DefaultEmbeddedCacheManager;
import org.jboss.as.clustering.infinispan.subsystem.EmbeddedCacheManagerConfiguration;
import org.jboss.as.clustering.infinispan.subsystem.EmbeddedCacheManagerConfigurationService;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Transition;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Infinispan-backed region factory for use with standalone (i.e. non-JPA) Hibernate applications.
 * @author Paul Ferraro
 */
public class InfinispanRegionFactory extends org.hibernate.cache.infinispan.InfinispanRegionFactory {

    public static final String CACHE_CONTAINER = "hibernate.cache.infinispan.container";
    public static final String DEFAULT_CACHE_CONTAINER = "hibernate";

    public InfinispanRegionFactory() {
        super();
    }

    public InfinispanRegionFactory(Properties props) {
        super(props);
    }

    @Override
    protected EmbeddedCacheManager createCacheManager(Properties properties) throws CacheException {
        String container = properties.getProperty(CACHE_CONTAINER, DEFAULT_CACHE_CONTAINER);
        ServiceName serviceName = EmbeddedCacheManagerConfigurationService.getServiceName(container);
        ServiceRegistry registry = CurrentServiceContainer.getServiceContainer();
        @SuppressWarnings("unchecked")
        ServiceController<EmbeddedCacheManagerConfiguration> service = (ServiceController<EmbeddedCacheManagerConfiguration>) registry.getRequiredService(serviceName);
        ServiceListener<EmbeddedCacheManagerConfiguration> listener = new NotifyingListener<EmbeddedCacheManagerConfiguration>();
        service.addListener(listener);
        synchronized (listener) {
            service.setMode(ServiceController.Mode.PASSIVE);
            try {
                while (EnumSet.of(ServiceController.State.DOWN, ServiceController.State.STARTING).contains(service.getState())) {
                    listener.wait();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        service.removeListener(listener);
        if (service.getState() != ServiceController.State.UP) {
            throw new CacheException(service.getStartException());
        }
        EmbeddedCacheManagerConfiguration config = service.getValue();
        GlobalConfiguration global = new GlobalConfigurationBuilder().read(config.getGlobalConfiguration()).classLoader(this.getClass().getClassLoader()).build();
        EmbeddedCacheManager manager = new DefaultEmbeddedCacheManager(global, config.getDefaultCache());
        manager.start();
        return manager;
    }

    static class NotifyingListener<T> extends AbstractServiceListener<T> {
        @Override
        public void transition(ServiceController<? extends T> controller, Transition transition) {
            synchronized (this) {
                this.notify();
            }
        }
    }
}

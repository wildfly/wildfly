/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.weld;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.BiFunction;

import org.jboss.weld.Container;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.config.WeldConfiguration;
import org.jboss.weld.event.GlobalObserverNotifierService;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.metadata.TypeStore;
import org.jboss.weld.module.ObserverNotifierFactory;
import org.jboss.weld.resources.ClassLoaderResourceLoader;
import org.jboss.weld.resources.ClassTransformer;
import org.jboss.weld.resources.ReflectionCache;
import org.jboss.weld.resources.SharedObjectCache;
import org.jboss.weld.resources.spi.ResourceLoader;

/**
 * @author Paul Ferraro
 */
public enum BeanManagerProvider implements BiFunction<String, String, BeanManagerImpl> {
    INSTANCE;

    @Override
    public BeanManagerImpl apply(String containerId, String id) {
        ServiceRegistry registry = mock(ServiceRegistry.class);
        WeldConfiguration configuration = mock(WeldConfiguration.class);
        ObserverNotifierFactory notifierFactory = mock(ObserverNotifierFactory.class);
        GlobalObserverNotifierService notifierService = mock(GlobalObserverNotifierService.class);
        ReflectionCache reflectionCache = mock(ReflectionCache.class);
        ResourceLoader loader = new ClassLoaderResourceLoader(this.getClass().getClassLoader());
        SharedObjectCache objectCache = new SharedObjectCache();
        TypeStore typeStore = new TypeStore();

        when(registry.get(WeldConfiguration.class)).thenReturn(configuration);
        when(registry.get(ObserverNotifierFactory.class)).thenReturn(notifierFactory);
        when(registry.get(GlobalObserverNotifierService.class)).thenReturn(notifierService);
        when(registry.get(SharedObjectCache.class)).thenReturn(objectCache);
        when(registry.get(ReflectionCache.class)).thenReturn(reflectionCache);
        when(registry.get(TypeStore.class)).thenReturn(typeStore);
        when(registry.get(ResourceLoader.class)).thenReturn(loader);
        when(registry.get(ClassTransformer.class)).thenReturn(new ClassTransformer(typeStore, objectCache, reflectionCache, containerId));

        BeanManagerImpl manager = BeanManagerImpl.newRootManager(containerId, id, registry);
        Container.initialize(containerId, manager, registry);
        return manager;
    }
}

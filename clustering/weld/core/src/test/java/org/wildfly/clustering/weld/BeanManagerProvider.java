/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

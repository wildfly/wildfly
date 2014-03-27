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
package org.wildfly.clustering.server.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.infinispan.configuration.cache.CacheMode;
import org.jboss.as.clustering.infinispan.CacheContainer;
import org.jboss.as.clustering.infinispan.subsystem.CacheServiceProvider;
import org.jboss.as.clustering.jgroups.subsystem.ChannelServiceProvider;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.naming.ManagedReferenceInjector;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.registry.RegistryFactory;

/**
 * Installs a {@link RegistryFactory} service per cache.
 * @author Paul Ferraro
 */
public class RegistryFactoryProvider implements CacheServiceProvider {
    private static final Logger logger = Logger.getLogger(ChannelServiceProvider.class);

    public static ServiceName getServiceName(String containerName, String cacheName) {
        return ServiceName.JBOSS.append("clustering", "registry", containerName, cacheName);
    }

    public static ServiceName getFactoryServiceName(String containerName, String cacheName) {
        return getServiceName(containerName, cacheName).append("factory");
    }

    public static ServiceName getEntryProviderServiceName(String containerName, String cacheName) {
        return getServiceName(containerName, cacheName).append("entry");
    }

    private static ContextNames.BindInfo createBinding(String containerName, String cacheName) {
        return ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, "clustering", "registry", containerName, cacheName).getAbsoluteName());
    }

    @Override
    public Collection<ServiceName> getServiceNames(String containerName, String cacheName, boolean defaultCache) {
        List<ServiceName> result = new ArrayList<>(4);
        result.add(getServiceName(containerName, cacheName));
        result.add(getFactoryServiceName(containerName, cacheName));
        result.add(createBinding(containerName, cacheName).getBinderServiceName());
        if (defaultCache && !cacheName.equals(CacheContainer.DEFAULT_CACHE_ALIAS)) {
            result.add(getServiceName(containerName, CacheContainer.DEFAULT_CACHE_ALIAS));
        }
        return result;
    }

    @Override
    public Collection<ServiceController<?>> install(ServiceTarget target, String containerName, String cacheName, CacheMode mode, boolean defaultCache, ModuleIdentifier moduleId) {
        ServiceName name = getFactoryServiceName(containerName, cacheName);
        ContextNames.BindInfo bindInfo = createBinding(containerName, cacheName);

        logger.debugf("Installing %s service, bound to ", name.getCanonicalName(), bindInfo.getAbsoluteJndiName());

        ServiceBuilder<RegistryFactory<Object, Object>> factoryBuilder = mode.isClustered() ? CacheRegistryFactoryService.build(target, name, containerName, cacheName) : LocalRegistryFactoryService.build(target, name, containerName, cacheName);

        ServiceBuilder<?> builder = RegistryService.build(target, containerName, cacheName);

        // Bind just the factory to JNDI
        BinderService binder = new BinderService(bindInfo.getBindName());
        @SuppressWarnings("rawtypes")
        ServiceBuilder<?> binderBuilder = target.addService(bindInfo.getBinderServiceName(), binder)
                .addAliases(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(bindInfo.getBindName()))
                .addDependency(name, RegistryFactory.class, new ManagedReferenceInjector<RegistryFactory>(binder.getManagedObjectInjector()))
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binder.getNamingStoreInjector())
                .setInitialMode(ServiceController.Mode.PASSIVE)
        ;

        boolean addDefaultCacheAlias = defaultCache && !cacheName.equals(CacheContainer.DEFAULT_CACHE_ALIAS);

        if (addDefaultCacheAlias) {
            ContextNames.BindInfo info = createBinding(containerName, CacheContainer.DEFAULT_CACHE_ALIAS);
            binderBuilder.addAliases(info.getBinderServiceName(), ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(info.getBindName()));
            factoryBuilder.addAliases(getFactoryServiceName(containerName, CacheContainer.DEFAULT_CACHE_ALIAS));
        }

        List<ServiceController<?>> result = new ArrayList<>(4);
        result.add(builder.setInitialMode(ServiceController.Mode.ON_DEMAND).install());
        result.add(factoryBuilder.setInitialMode(ServiceController.Mode.ON_DEMAND).install());
        result.add(binderBuilder.install());

        if (addDefaultCacheAlias) {
            ServiceBuilder<?> defaultBuilder = RegistryService.build(target, containerName, CacheContainer.DEFAULT_CACHE_ALIAS);
            result.add(defaultBuilder.setInitialMode(ServiceController.Mode.ON_DEMAND).install());
        }

        return result;
    }
}

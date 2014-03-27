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
package org.wildfly.clustering.server.provider;

import java.util.Arrays;
import java.util.Collection;

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
import org.wildfly.clustering.provider.ServiceProviderRegistrationFactory;

/**
 * Installs a ServiceProviderRegistry service per cache.
 * @author Paul Ferraro
 */
public class ServiceProviderRegistrationFactoryProvider implements CacheServiceProvider {
    private static final Logger logger = Logger.getLogger(ChannelServiceProvider.class);

    public static ServiceName getServiceName(String containerName, String cacheName) {
        return ServiceName.JBOSS.append("clustering", "providers", containerName, cacheName);
    }

    private static ContextNames.BindInfo createBinding(String containerName, String cacheName) {
        return ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, "clustering", "providers", containerName, cacheName).getAbsoluteName());
    }

    @Override
    public Collection<ServiceName> getServiceNames(String containerName, String cacheName, boolean defaultCache) {
        return Arrays.asList(getServiceName(containerName, cacheName), createBinding(containerName, cacheName).getBinderServiceName());
    }

    @Override
    public Collection<ServiceController<?>> install(ServiceTarget target, String containerName, String cacheName, CacheMode mode, boolean defaultCache, ModuleIdentifier moduleId) {
        ServiceName name = getServiceName(containerName, cacheName);
        ContextNames.BindInfo bindInfo = createBinding(containerName, cacheName);

        logger.debugf("Installing %s service, bound to ", name.getCanonicalName(), bindInfo.getAbsoluteJndiName());

        ServiceBuilder<ServiceProviderRegistrationFactory> builder = mode.isClustered() ? CacheServiceProviderRegistrationFactoryService.build(target, name, containerName, cacheName) : LocalServiceProviderRegistrationFactoryService.build(target, name, containerName, cacheName);

        BinderService binder = new BinderService(bindInfo.getBindName());
        ServiceBuilder<?> binderBuilder = target.addService(bindInfo.getBinderServiceName(), binder)
                .addAliases(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(bindInfo.getBindName()))
                .addDependency(name, ServiceProviderRegistrationFactory.class, new ManagedReferenceInjector<ServiceProviderRegistrationFactory>(binder.getManagedObjectInjector()))
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binder.getNamingStoreInjector())
                .setInitialMode(ServiceController.Mode.PASSIVE)
        ;

        if (defaultCache) {
            ContextNames.BindInfo info = createBinding(containerName, CacheContainer.DEFAULT_CACHE_ALIAS);
            binderBuilder.addAliases(info.getBinderServiceName(), ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(info.getBindName()));
            builder.addAliases(getServiceName(containerName, CacheContainer.DEFAULT_CACHE_ALIAS));
        }

        ServiceController<ServiceProviderRegistrationFactory> controller = builder.setInitialMode(ServiceController.Mode.ON_DEMAND).install();

        return Arrays.asList(controller, binderBuilder.install());
    }
}

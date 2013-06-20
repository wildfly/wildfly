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
package org.wildfly.clustering.server.service;

import java.util.Arrays;
import java.util.Collection;

import org.infinispan.Cache;
import org.jboss.as.clustering.infinispan.invoker.BatchCacheInvoker;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.infinispan.subsystem.CacheService;
import org.jboss.as.clustering.infinispan.subsystem.ChannelDependentServiceProvider;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.naming.ManagedReferenceInjector;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactoryProvider;
import org.wildfly.clustering.service.ServiceProviderRegistry;

/**
 * Installs a ServiceProviderRegistry service per channel.
 * @author Paul Ferraro
 */
public class ServiceProviderRegistryFactoryProvider implements ChannelDependentServiceProvider {
    private static final Logger logger = Logger.getLogger(CommandDispatcherFactoryProvider.class);

    private ServiceName getServiceName(String cluster) {
        return ServiceProviderRegistry.SERVICE_NAME.append(cluster);
    }

    @Override
    public Collection<ServiceName> getServiceNames(String cluster) {
        return Arrays.asList(this.getServiceName(cluster), this.createBinding(cluster).getBinderServiceName());
    }

    @Override
    public Collection<ServiceController<?>> install(ServiceTarget target, String cluster, ModuleIdentifier moduleId) {
        ServiceName name = this.getServiceName(cluster);
        ContextNames.BindInfo bindInfo = this.createBinding(cluster);

        logger.debugf("Installing %s service, bound to ", name.getCanonicalName(), bindInfo.getAbsoluteJndiName());

        BinderService binder = new BinderService(bindInfo.getBindName());
        ServiceController<?> binderController = target.addService(bindInfo.getBinderServiceName(), binder)
                .addAliases(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(bindInfo.getBindName()))
                .addDependency(name, ServiceProviderRegistry.class, new ManagedReferenceInjector<ServiceProviderRegistry>(binder.getManagedObjectInjector()))
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binder.getNamingStoreInjector())
                .setInitialMode(ServiceController.Mode.PASSIVE)
                .install()
        ;

        @SuppressWarnings("rawtypes")
        InjectedValue<Cache> cache = new InjectedValue<>();
        InjectedValue<CommandDispatcherFactory> dispatcherFactory = new InjectedValue<>();
        CacheInvoker invoker = new BatchCacheInvoker();
        ServiceController<?> controller = target.addService(name, new ServiceProviderRegistryFactoryService(name, cache, dispatcherFactory, invoker))
                .addDependency(CacheService.getServiceName(cluster, null), Cache.class, cache)
                .addDependency(CommandDispatcherFactory.SERVICE_NAME.append(cluster), CommandDispatcherFactory.class, dispatcherFactory)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install()
        ;
        return Arrays.asList(controller, binderController);
    }

    private ContextNames.BindInfo createBinding(String cluster) {
        return ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, "clustering", "registry", cluster).getAbsoluteName());
    }
}

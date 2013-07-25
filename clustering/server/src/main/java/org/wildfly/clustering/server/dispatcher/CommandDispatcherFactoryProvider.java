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
package org.wildfly.clustering.server.dispatcher;

import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.clustering.infinispan.subsystem.CacheService;
import org.jboss.as.clustering.infinispan.subsystem.ChannelDependentServiceProvider;
import org.jboss.as.clustering.jgroups.subsystem.ChannelService;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.naming.ManagedReferenceInjector;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.Services;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.jgroups.Channel;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;

/**
 * Install a CommandDispatcherFactory per channel.
 * @author Paul Ferraro
 */
public class CommandDispatcherFactoryProvider implements ChannelDependentServiceProvider {
    private static final Logger logger = Logger.getLogger(CommandDispatcherFactoryProvider.class);

    private static ServiceName getServiceName(String cluster) {
        return CommandDispatcherFactory.SERVICE_NAME.append(cluster);
    }

    @Override
    public Collection<ServiceName> getServiceNames(String cluster) {
        return Arrays.asList(getServiceName(cluster), createBinding(cluster).getBinderServiceName());
    }

    @Override
    public Collection<ServiceController<?>> install(ServiceTarget target, String cluster, ModuleIdentifier moduleId) {
        ServiceName name = getServiceName(cluster);
        ContextNames.BindInfo bindInfo = createBinding(cluster);

        logger.debugf("Installing %s service, bound to ", name.getCanonicalName(), bindInfo.getAbsoluteJndiName());

        BinderService binder = new BinderService(bindInfo.getBindName());
        ServiceController<?> binderController = target.addService(bindInfo.getBinderServiceName(), binder)
                .addAliases(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(bindInfo.getBindName()))
                .addDependency(name, CommandDispatcherFactory.class, new ManagedReferenceInjector<CommandDispatcherFactory>(binder.getManagedObjectInjector()))
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binder.getNamingStoreInjector())
                .setInitialMode(ServiceController.Mode.PASSIVE)
                .install()
        ;

        InjectedValue<Channel> channel = new InjectedValue<>();
        InjectedValue<ModuleLoader> loader = new InjectedValue<>();
        Service<CommandDispatcherFactory> service = new CommandDispatcherFactoryService(channel, loader, moduleId);
        ServiceController<?> controller = target.addService(name, service)
                // Make sure Infinispan starts its channel before we try to use it..
                .addDependency(CacheService.getServiceName(cluster, null))
                .addDependency(ChannelService.getServiceName(cluster), Channel.class, channel)
                .addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, loader)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install()
        ;
        return Arrays.asList(controller, binderController);
    }

    private static ContextNames.BindInfo createBinding(String cluster) {
        return ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, "clustering", "dispatcher", cluster).getAbsoluteName());
    }
}

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

import org.jboss.as.clustering.infinispan.subsystem.GlobalComponentRegistryService;
import org.jboss.as.clustering.jgroups.subsystem.ChannelService;
import org.jboss.as.clustering.jgroups.subsystem.ChannelServiceProvider;
import org.jboss.as.clustering.msc.AsynchronousService;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.naming.ManagedReferenceInjector;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.Services;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.jgroups.Channel;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.server.group.ChannelGroupProvider;
import org.wildfly.clustering.server.group.ChannelNodeFactory;
import org.wildfly.clustering.server.group.ChannelNodeFactoryProvider;

/**
 * Installs a {@link CommandDispatcherFactory} service per channel.
 * @author Paul Ferraro
 */
public class CommandDispatcherFactoryProvider implements ChannelServiceProvider {
    private static final Logger logger = Logger.getLogger(ChannelServiceProvider.class);

    public static ServiceName getServiceName(String cluster) {
        return ServiceName.JBOSS.append("clustering", "dispatcher").append(cluster);
    }

    private static ContextNames.BindInfo createBinding(String cluster) {
        return ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, "clustering", "dispatcher", cluster).getAbsoluteName());
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

        CommandDispatcherFactoryConfig config = new CommandDispatcherFactoryConfig(moduleId);
        Service<CommandDispatcherFactory> service = new CommandDispatcherFactoryService(config);
        ServiceBuilder<?> builder = AsynchronousService.addService(target, name, service)
                // Make sure Infinispan starts its channel before we try to use it.
                .addDependency(GlobalComponentRegistryService.getServiceName(cluster))
                .addDependency(ChannelGroupProvider.getServiceName(cluster), Group.class, config.getGroupInjector())
                .addDependency(ChannelNodeFactoryProvider.getServiceName(cluster), ChannelNodeFactory.class, config.getNodeFactoryInjector())
                .addDependency(ChannelService.getServiceName(cluster), Channel.class, config.getChannelInjector())
                .addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, config.getModuleLoaderInjector())
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
        ;

        BinderService binder = new BinderService(bindInfo.getBindName());
        ServiceBuilder<?> binderBuilder = target.addService(bindInfo.getBinderServiceName(), binder)
                .addAliases(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(bindInfo.getBindName()))
                .addDependency(name, CommandDispatcherFactory.class, new ManagedReferenceInjector<CommandDispatcherFactory>(binder.getManagedObjectInjector()))
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binder.getNamingStoreInjector())
                .setInitialMode(ServiceController.Mode.PASSIVE)
        ;

        return Arrays.asList(builder.install(), binderBuilder.install());
    }

    private static class CommandDispatcherFactoryConfig implements CommandDispatcherFactoryConfiguration {
        private final InjectedValue<Channel> channel = new InjectedValue<>();
        private final InjectedValue<Group> group = new InjectedValue<>();
        private final InjectedValue<ChannelNodeFactory> factory = new InjectedValue<>();
        private final InjectedValue<ModuleLoader> loader = new InjectedValue<>();

        private final ModuleIdentifier identifier;

        CommandDispatcherFactoryConfig(ModuleIdentifier identifier) {
            this.identifier = identifier;
        }

        @Override
        public Channel getChannel() {
            return this.channel.getValue();
        }

        @Override
        public Group getGroup() {
            return this.group.getValue();
        }

        @Override
        public ChannelNodeFactory getNodeFactory() {
            return this.factory.getValue();
        }

        @Override
        public ModuleLoader getModuleLoader() {
            return this.loader.getValue();
        }

        @Override
        public ModuleIdentifier getModuleIdentifier() {
            return this.identifier;
        }

        Injector<Channel> getChannelInjector() {
            return this.channel;
        }

        Injector<Group> getGroupInjector() {
            return this.group;
        }

        Injector<ChannelNodeFactory> getNodeFactoryInjector() {
            return this.factory;
        }

        Injector<ModuleLoader> getModuleLoaderInjector() {
            return this.loader;
        }
    }
}

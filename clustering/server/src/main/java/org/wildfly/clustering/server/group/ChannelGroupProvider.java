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
package org.wildfly.clustering.server.group;

import java.util.Arrays;
import java.util.Collection;

import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.infinispan.subsystem.EmbeddedCacheManagerService;
import org.jboss.as.clustering.infinispan.subsystem.GlobalComponentRegistryService;
import org.jboss.as.clustering.jgroups.subsystem.ChannelServiceProvider;
import org.jboss.as.clustering.msc.AsynchronousService;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.naming.ManagedReferenceInjector;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.group.Group;

/**
 * Installs a {@link Group} service per channel.
 * @author Paul Ferraro
 */
public class ChannelGroupProvider implements ChannelServiceProvider {
    private static final Logger logger = Logger.getLogger(ChannelServiceProvider.class);

    public static ServiceName getServiceName(String cluster) {
        return ServiceName.JBOSS.append("clustering", "group", cluster);
    }

    private static ContextNames.BindInfo createBinding(String cluster) {
        return ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, "clustering", "group", cluster).getAbsoluteName());
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

        ChannelGroupConfig config = new ChannelGroupConfig();
        Service<Group> service = new ChannelGroupService(config);
        ServiceBuilder<?> builder = AsynchronousService.addService(target, name, service)
                // Make sure Infinispan starts its channel before we try to use it..
                .addDependency(GlobalComponentRegistryService.getServiceName(cluster))
                .addDependency(EmbeddedCacheManagerService.getServiceName(cluster), EmbeddedCacheManager.class, config.getCacheManagerInjector())
                .addDependency(ChannelNodeFactoryProvider.getServiceName(cluster), ChannelNodeFactory.class, config.getNodeFactoryInjector())
                .setInitialMode(ServiceController.Mode.PASSIVE)
        ;

        BinderService binder = new BinderService(bindInfo.getBindName());
        ServiceBuilder<?> binderBuilder = target.addService(bindInfo.getBinderServiceName(), binder)
                .addAliases(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(bindInfo.getBindName()))
                .addDependency(name, Group.class, new ManagedReferenceInjector<Group>(binder.getManagedObjectInjector()))
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binder.getNamingStoreInjector())
                .setInitialMode(ServiceController.Mode.PASSIVE)
        ;

        return Arrays.asList(builder.install(), binderBuilder.install());
    }

    static class ChannelGroupConfig implements ChannelGroupConfiguration {
        private final InjectedValue<EmbeddedCacheManager> manager = new InjectedValue<>();
        private final InjectedValue<ChannelNodeFactory> factory = new InjectedValue<>();

        @Override
        public EmbeddedCacheManager getCacheContainer() {
            return this.manager.getValue();
        }

        @Override
        public ChannelNodeFactory getNodeFactory() {
            return this.factory.getValue();
        }

        Injector<EmbeddedCacheManager> getCacheManagerInjector() {
            return this.manager;
        }

        Injector<ChannelNodeFactory> getNodeFactoryInjector() {
            return this.factory;
        }
    }
}

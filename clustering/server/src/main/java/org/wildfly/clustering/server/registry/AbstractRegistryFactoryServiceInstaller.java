/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.clustering.infinispan.CacheContainer;
import org.jboss.as.clustering.naming.BinderServiceBuilder;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.registry.RegistryFactory;
import org.wildfly.clustering.spi.CacheServiceNames;
import org.wildfly.clustering.spi.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractRegistryFactoryServiceInstaller implements ServiceInstaller {
    private final Logger logger = Logger.getLogger(this.getClass());

    private static ContextNames.BindInfo createBinding(String group) {
        return ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, "clustering", "registry", group, CacheContainer.DEFAULT_CACHE_ALIAS).getAbsoluteName());
    }

    @Override
    public Collection<ServiceName> getServiceNames(String group) {
        return Arrays.asList(CacheServiceNames.REGISTRY.getServiceName(group), CacheServiceNames.REGISTRY_FACTORY.getServiceName(group), createBinding(group).getBinderServiceName());
    }

    @Override
    public Collection<ServiceController<?>> install(ServiceTarget target, String group, ModuleIdentifier moduleId) {
        ServiceName name = CacheServiceNames.REGISTRY_FACTORY.getServiceName(group);
        ContextNames.BindInfo bindInfo = createBinding(group);

        this.logger.debugf("Installing %s service, bound to ", name.getCanonicalName(), bindInfo.getAbsoluteJndiName());

        ServiceBuilder<RegistryFactory<Object, Object>> factoryBuilder = this.build(target, name, group, CacheContainer.DEFAULT_CACHE_ALIAS);
        ServiceController<RegistryFactory<Object, Object>> factoryController = factoryBuilder.setInitialMode(ServiceController.Mode.ON_DEMAND).install();

        ServiceBuilder<Registry<Object, Object>> builder = RegistryService.build(target, group, CacheContainer.DEFAULT_CACHE_ALIAS);
        ServiceController<Registry<Object, Object>> controller = builder.setInitialMode(ServiceController.Mode.ON_DEMAND).install();

        ServiceBuilder<ManagedReferenceFactory> binderBuilder = new BinderServiceBuilder(target).build(bindInfo, name, RegistryFactory.class);

        return Arrays.asList(factoryController, controller, binderBuilder.install());
    }

    protected abstract ServiceBuilder<RegistryFactory<Object, Object>> build(ServiceTarget target, ServiceName name, String container, String cache);
}

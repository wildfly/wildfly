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

import org.jboss.as.clustering.naming.BinderServiceBuilder;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.registry.RegistryFactory;
import org.wildfly.clustering.server.CacheServiceBuilder;
import org.wildfly.clustering.spi.CacheServiceInstaller;
import org.wildfly.clustering.spi.CacheServiceNames;
import org.wildfly.clustering.spi.GroupServiceNameFactory;

/**
 * @author Paul Ferraro
 */
public class RegistryFactoryServiceInstaller implements CacheServiceInstaller {
    private final Logger logger = Logger.getLogger(this.getClass());

    private final CacheServiceBuilder<RegistryFactory<Object, Object>> builder;

    protected RegistryFactoryServiceInstaller(CacheServiceBuilder<RegistryFactory<Object, Object>> builder) {
        this.builder = builder;
    }

    private static ContextNames.BindInfo createBinding(String container, String cache) {
        return ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, GroupServiceNameFactory.BASE_NAME, CacheServiceNames.REGISTRY.toString(), container, cache).getAbsoluteName());
    }

    @Override
    public Collection<ServiceName> getServiceNames(String container, String cache) {
        return Arrays.asList(CacheServiceNames.REGISTRY.getServiceName(container, cache), CacheServiceNames.REGISTRY_FACTORY.getServiceName(container, cache), createBinding(container, cache).getBinderServiceName());
    }

    @Override
    public Collection<ServiceController<?>> install(ServiceTarget target, String container, String cache) {
        ServiceName name = CacheServiceNames.REGISTRY_FACTORY.getServiceName(container, cache);
        ContextNames.BindInfo bindInfo = createBinding(container, cache);

        this.logger.debugf("Installing %s service, bound to ", name.getCanonicalName(), bindInfo.getAbsoluteJndiName());

        ServiceBuilder<RegistryFactory<Object, Object>> factoryBuilder = this.builder.build(target, name, container, cache).setInitialMode(ServiceController.Mode.ON_DEMAND);

        ServiceBuilder<Registry<Object, Object>> builder = RegistryService.build(target, container, cache).setInitialMode(ServiceController.Mode.ON_DEMAND);

        ServiceBuilder<ManagedReferenceFactory> binderBuilder = new BinderServiceBuilder(target).build(bindInfo, name, RegistryFactory.class);

        return Arrays.asList(factoryBuilder.install(), builder.install(), binderBuilder.install());
    }
}

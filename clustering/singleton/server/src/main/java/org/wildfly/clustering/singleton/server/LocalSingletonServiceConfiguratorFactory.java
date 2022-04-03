/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.singleton.server;

import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.singleton.service.SingletonServiceConfigurator;
import org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory;

/**
 * Factory for creating local {@link SingletonServiceConfigurator} instances.
 * @author Paul Ferraro
 */
public class LocalSingletonServiceConfiguratorFactory implements SingletonServiceConfiguratorFactory, LocalSingletonServiceConfiguratorContext {

    private final LocalSingletonServiceConfiguratorFactoryContext context;

    public LocalSingletonServiceConfiguratorFactory(LocalSingletonServiceConfiguratorFactoryContext context) {
        this.context = context;
    }

    @Override
    public SingletonServiceConfigurator createSingletonServiceConfigurator(ServiceName name) {
        return new LocalSingletonServiceConfigurator(name, this);
    }

    @Override
    public SupplierDependency<Group> getGroupDependency() {
        return new ServiceSupplierDependency<>(this.context.getGroupServiceName());
    }
}

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
package org.wildfly.clustering.server.provider;


import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.provider.ServiceProviderRegistrationFactory;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.spi.CacheGroupServiceName;

/**
 * Builds a non-clustered {@link ServiceProviderRegistrationFactory} service.
 * @author Paul Ferraro
 */
public class LocalServiceProviderRegistrationFactoryBuilder extends ServiceProviderRegistrationFactoryServiceNameProvider implements Builder<ServiceProviderRegistrationFactory>, Value<ServiceProviderRegistrationFactory> {

    private final InjectedValue<Group> group = new InjectedValue<>();

    /**
     * @param containerName
     * @param cacheName
     */
    public LocalServiceProviderRegistrationFactoryBuilder(String containerName, String cacheName) {
        super(containerName, cacheName);
    }

    @Override
    public ServiceBuilder<ServiceProviderRegistrationFactory> build(ServiceTarget target) {
        return target.addService(this.getServiceName(), new ValueService<>(this))
                .addDependency(CacheGroupServiceName.GROUP.getServiceName(this.containerName, this.cacheName), Group.class, this.group)
                .setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public ServiceProviderRegistrationFactory getValue() {
        return new LocalServiceProviderRegistrationFactory(this.group.getValue());
    }
}

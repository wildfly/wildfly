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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.provider.ServiceProviderRegistration;
import org.wildfly.clustering.provider.ServiceProviderRegistration.Listener;
import org.wildfly.clustering.provider.ServiceProviderRegistrationFactory;
import org.wildfly.clustering.spi.CacheServiceNames;

/**
 * Factory that provides a non-clustered {@link ServiceProviderRegistrationFactory} implementation.
 * @author Paul Ferraro
 */
public class LocalServiceProviderRegistrationFactoryService extends AbstractService<ServiceProviderRegistrationFactory> implements ServiceProviderRegistrationFactory {

    public static ServiceBuilder<ServiceProviderRegistrationFactory> build(ServiceTarget target, ServiceName name, String containerName, String cacheName) {
        LocalServiceProviderRegistrationFactoryService service = new LocalServiceProviderRegistrationFactoryService();
        return target.addService(name, service)
                .addDependency(CacheServiceNames.GROUP.getServiceName(containerName, cacheName), Group.class, service.group)
        ;
    }

    final Set<Object> services = Collections.synchronizedSet(new HashSet<>());

    private final InjectedValue<Group> group = new InjectedValue<>();

    private LocalServiceProviderRegistrationFactoryService() {
        // Hide
    }

    @Override
    public ServiceProviderRegistrationFactory getValue() {
        return this;
    }

    @Override
    public Group getGroup() {
        return this.group.getValue();
    }

    @Override
    public ServiceProviderRegistration createRegistration(final Object service, Listener listener) {
        this.services.add(service);
        return new AbstractServiceProviderRegistration(service, this) {
            @Override
            public void close() {
                LocalServiceProviderRegistrationFactoryService.this.services.remove(service);
            }
        };
    }

    @Override
    public Set<Node> getProviders(Object service) {
        return this.services.contains(service) ? Collections.singleton(this.getGroup().getLocalNode()) : Collections.<Node>emptySet();
    }
}

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
package org.wildfly.clustering.server.singleton;

import java.io.Serializable;

import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.SingletonServiceBuilder;
import org.wildfly.clustering.singleton.SingletonServiceBuilderFactory;

/**
 * Service for building {@link SingletonService} instances.
 * @author Paul Ferraro
 */
public class CacheSingletonServiceBuilderFactoryService extends AbstractService<SingletonServiceBuilderFactory> implements SingletonServiceBuilderFactory {

    final String containerName;
    final String cacheName;

    public CacheSingletonServiceBuilderFactoryService(String containerName, String cacheName) {
        this.containerName = containerName;
        this.cacheName = cacheName;
    }

    @Override
    public SingletonServiceBuilderFactory getValue() {
        return this;
    }

    @Override
    public <T extends Serializable> SingletonServiceBuilder<T> createSingletonServiceBuilder(ServiceName name, Service<T> service) {
        final SingletonService<T> singleton = new SingletonService<>(name, service);
        return new SingletonServiceBuilder<T>() {
            @Override
            public SingletonServiceBuilder<T> requireQuorum(int quorum) {
                singleton.setQuorum(quorum);
                return this;
            }

            @Override
            public SingletonServiceBuilder<T> electionPolicy(SingletonElectionPolicy policy) {
                singleton.setElectionPolicy(policy);
                return this;
            }

            @Override
            public ServiceBuilder<T> build(ServiceTarget target) {
                return singleton.build(target, CacheSingletonServiceBuilderFactoryService.this.containerName, CacheSingletonServiceBuilderFactoryService.this.cacheName);
            }
        };
    }
}

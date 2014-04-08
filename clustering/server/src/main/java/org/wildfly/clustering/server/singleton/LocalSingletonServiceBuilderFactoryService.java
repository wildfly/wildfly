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
 * Service that provides a non-clustered {@link SingletonServiceBuilderFactory}
 * @author Paul Ferraro
 */
public class LocalSingletonServiceBuilderFactoryService extends AbstractService<SingletonServiceBuilderFactory> implements SingletonServiceBuilderFactory {

    @Override
    public SingletonServiceBuilderFactory getValue() {
        return this;
    }

    @Override
    public <T extends Serializable> SingletonServiceBuilder<T> createSingletonServiceBuilder(final ServiceName name, final Service<T> service) {
        return new SingletonServiceBuilder<T>() {
            @Override
            public SingletonServiceBuilder<T> requireQuorum(int quorum) {
                // Quorum requirements are inconsequential to a local singleton
                return this;
            }

            @Override
            public SingletonServiceBuilder<T> electionPolicy(SingletonElectionPolicy policy) {
                // Election policies are inconsequential to a local singleton
                return this;
            }

            @Override
            public ServiceBuilder<T> build(ServiceTarget target) {
                return target.addService(name, service);
            }
        };
    }
}

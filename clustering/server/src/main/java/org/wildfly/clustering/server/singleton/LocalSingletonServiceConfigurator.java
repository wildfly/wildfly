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

package org.wildfly.clustering.server.singleton;

import org.jboss.msc.Service;
import org.jboss.msc.service.DelegatingServiceBuilder;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.service.SingletonServiceBuilder;
import org.wildfly.clustering.singleton.service.SingletonServiceConfigurator;

/**
 * Local {@link SingletonServiceConfigurator} implementation that uses JBoss MSC 1.4.x service installation.
 * @author Paul Ferraro
 */
public class LocalSingletonServiceConfigurator extends SimpleServiceNameProvider implements SingletonServiceConfigurator {

    public LocalSingletonServiceConfigurator(ServiceName name) {
        super(name);
    }

    @Override
    public SingletonServiceBuilder<?> build(ServiceTarget target) {
        return new LocalSingletonServiceBuilder<>(target.addService(this.getServiceName()));
    }

    @Override
    public SingletonServiceConfigurator requireQuorum(int quorum) {
        // Quorum requirements are inconsequential to a local singleton
        return this;
    }

    @Override
    public SingletonServiceConfigurator electionPolicy(SingletonElectionPolicy policy) {
        // Election policies are inconsequential to a local singleton
        return this;
    }

    private static class LocalSingletonServiceBuilder<T> extends DelegatingServiceBuilder<T> implements SingletonServiceBuilder<T> {

        LocalSingletonServiceBuilder(ServiceBuilder<T> builder) {
            super(builder);
        }

        @Override
        public ServiceBuilder<T> setInstance(Service service) {
            return super.setInstance(new LocalSingletonService(service));
        }
    }
}

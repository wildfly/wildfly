/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.singleton.service;

import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.singleton.SingletonServiceBuilderFactory;
import org.wildfly.clustering.singleton.SingletonServiceName;
import org.wildfly.clustering.singleton.election.NamePreference;
import org.wildfly.clustering.singleton.election.PreferredSingletonElectionPolicy;
import org.wildfly.clustering.singleton.election.SimpleSingletonElectionPolicy;

import static org.jboss.as.test.clustering.ClusteringTestConstants.NODE_2;

import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;

/**
 * @author Paul Ferraro
 */
public class MyServiceActivator implements ServiceActivator {

    public static final ServiceName DEFAULT_SERVICE_NAME = ServiceName.JBOSS.append("test", "myservice", "default");
    public static final ServiceName QUORUM_SERVICE_NAME = ServiceName.JBOSS.append("test", "myservice", "quorum");

    private static final String CONTAINER_NAME = "server";
    public static final String PREFERRED_NODE = NODE_2;

    @Override
    public void activate(ServiceActivatorContext context) {
        ServiceTarget target = context.getServiceTarget();
        try {
            SingletonServiceBuilderFactory factory = (SingletonServiceBuilderFactory) context.getServiceRegistry().getRequiredService(SingletonServiceName.BUILDER.getServiceName(CONTAINER_NAME)).awaitValue();
            install(target, factory, DEFAULT_SERVICE_NAME, 1);
            install(target, factory, QUORUM_SERVICE_NAME, 2);
        } catch (InterruptedException e) {
            throw new ServiceRegistryException(e);
        }
    }

    private static void install(ServiceTarget target, SingletonServiceBuilderFactory factory, ServiceName name, int quorum) {
        InjectedValue<ServerEnvironment> env = new InjectedValue<>();
        MyService service = new MyService(env);
        factory.createSingletonServiceBuilder(name, service)
            .electionPolicy(new PreferredSingletonElectionPolicy(new SimpleSingletonElectionPolicy(), new NamePreference(PREFERRED_NODE)))
            .requireQuorum(quorum)
            .build(target)
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, env)
                .install();
    }
}

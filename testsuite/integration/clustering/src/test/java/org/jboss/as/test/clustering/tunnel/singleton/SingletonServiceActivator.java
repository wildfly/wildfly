/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.tunnel.singleton;

import org.jboss.as.test.clustering.cluster.singleton.service.MyService;
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

import static org.jboss.as.test.clustering.ClusteringTestConstants.NODE_1;
import static org.jboss.as.test.clustering.ClusteringTestConstants.NODE_2;

import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;

/**
 * @author Tomas Hofman
 */
public class SingletonServiceActivator implements ServiceActivator {

    private static final String CONTAINER_NAME = "server";
    public static final ServiceName SERVICE_A_NAME = ServiceName.JBOSS.append("test1", "myservice", "default");
    public static final ServiceName SERVICE_B_NAME = ServiceName.JBOSS.append("test2", "myservice", "default");
    public static final String SERVICE_A_PREFERRED_NODE = NODE_2;
    public static final String SERVICE_B_PREFERRED_NODE = NODE_1;

    @Override
    public void activate(ServiceActivatorContext context) {
        try {
            SingletonServiceBuilderFactory factory = (SingletonServiceBuilderFactory) context.getServiceRegistry().getRequiredService(SingletonServiceName.BUILDER.getServiceName(CONTAINER_NAME)).awaitValue();
            ServiceTarget target = context.getServiceTarget();
            install(target, factory, SERVICE_A_NAME, SERVICE_A_PREFERRED_NODE);
            install(target, factory, SERVICE_B_NAME, SERVICE_B_PREFERRED_NODE);
        } catch (InterruptedException e) {
            throw new ServiceRegistryException(e);
        }
    }

    private static void install(ServiceTarget target, SingletonServiceBuilderFactory factory, ServiceName name, String preferredNode) {
        InjectedValue<ServerEnvironment> env = new InjectedValue<>();
        MyService service = new MyService(env);
        factory.createSingletonServiceBuilder(name, service)
            .electionPolicy(new PreferredSingletonElectionPolicy(new SimpleSingletonElectionPolicy(), new NamePreference(preferredNode)))
            .build(target)
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, env)
                .install();
    }

}

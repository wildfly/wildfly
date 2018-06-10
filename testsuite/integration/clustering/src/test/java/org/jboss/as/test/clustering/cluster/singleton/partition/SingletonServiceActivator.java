/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.clustering.cluster.singleton.partition;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.NODE_1;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.NODE_2;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.service.ActiveServiceSupplier;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.singleton.SingletonDefaultCacheRequirement;
import org.wildfly.clustering.singleton.election.NamePreference;
import org.wildfly.clustering.singleton.election.PreferredSingletonElectionPolicy;
import org.wildfly.clustering.singleton.election.SimpleSingletonElectionPolicy;
import org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory;

/**
 * @author Tomas Hofman
 */
public class SingletonServiceActivator implements ServiceActivator {

    private static final String CONTAINER_NAME = "server";
    public static final ServiceName SERVICE_A_NAME = ServiceName.JBOSS.append("test1", "service", "default");
    public static final ServiceName SERVICE_B_NAME = ServiceName.JBOSS.append("test2", "service", "default");
    public static final String SERVICE_A_PREFERRED_NODE = NODE_2;
    public static final String SERVICE_B_PREFERRED_NODE = NODE_1;

    @Override
    public void activate(ServiceActivatorContext context) {
        SingletonServiceConfiguratorFactory factory = new ActiveServiceSupplier<SingletonServiceConfiguratorFactory>(context.getServiceRegistry(), ServiceName.parse(SingletonDefaultCacheRequirement.SINGLETON_SERVICE_CONFIGURATOR_FACTORY.resolve(CONTAINER_NAME))).get();
        ServiceTarget target = context.getServiceTarget();
        install(target, factory, SERVICE_A_NAME, SERVICE_A_PREFERRED_NODE);
        install(target, factory, SERVICE_B_NAME, SERVICE_B_PREFERRED_NODE);
    }

    private static void install(ServiceTarget target, SingletonServiceConfiguratorFactory factory, ServiceName name, String preferredNode) {
        ServiceBuilder<?> builder = factory.createSingletonServiceConfigurator(name)
                .electionPolicy(new PreferredSingletonElectionPolicy(new SimpleSingletonElectionPolicy(), new NamePreference(preferredNode)))
                .build(target);
        Consumer<Node> member = builder.provides(name);
        Supplier<Group> group = builder.requires(ServiceName.parse("org.wildfly.clustering.default-group"));
        Service service = new FunctionalService<>(member, Group::getLocalMember, group);
        builder.setInstance(service).install();
    }
}

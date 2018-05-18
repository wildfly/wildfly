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
 * @author Paul Ferraro
 */
public class NodeServiceActivator implements ServiceActivator {

    public static final ServiceName DEFAULT_SERVICE_NAME = ServiceName.JBOSS.append("test", "service", "default");
    public static final ServiceName QUORUM_SERVICE_NAME = ServiceName.JBOSS.append("test", "service", "quorum");

    private static final String CONTAINER_NAME = "server";
    public static final String PREFERRED_NODE = NODE_2;

    @Override
    public void activate(ServiceActivatorContext context) {
        ServiceTarget target = context.getServiceTarget();
        SingletonServiceConfiguratorFactory factory = new ActiveServiceSupplier<SingletonServiceConfiguratorFactory>(context.getServiceRegistry(), ServiceName.parse(SingletonDefaultCacheRequirement.SINGLETON_SERVICE_CONFIGURATOR_FACTORY.resolve(CONTAINER_NAME))).get();
        install(target, factory, DEFAULT_SERVICE_NAME, 1);
        install(target, factory, QUORUM_SERVICE_NAME, 2);
    }

    private static void install(ServiceTarget target, SingletonServiceConfiguratorFactory factory, ServiceName name, int quorum) {
        ServiceBuilder<?> builder = factory.createSingletonServiceConfigurator(name)
                .electionPolicy(new PreferredSingletonElectionPolicy(new SimpleSingletonElectionPolicy(), new NamePreference(PREFERRED_NODE)))
                .requireQuorum(quorum)
                .build(target);
        Consumer<Node> member = builder.provides(name);
        Supplier<Group> group = builder.requires(ServiceName.parse("org.wildfly.clustering.default-group"));
        Service service = new FunctionalService<>(member, Group::getLocalMember, group);
        builder.setInstance(service).install();
    }
}

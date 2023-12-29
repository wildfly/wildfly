/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.singleton.partition;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.NODE_1;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.NODE_2;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.test.clustering.cluster.singleton.service.NodeServiceExecutorRegistry;
import org.jboss.as.test.clustering.cluster.singleton.service.SingletonElectionListenerService;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.ChildTargetService;
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
        ServiceBuilder<?> builder = context.getServiceTarget().addService(ServiceName.JBOSS.append("test1", "service", "installer"));
        Supplier<SingletonServiceConfiguratorFactory> factoryDependency = builder.requires(ServiceName.parse(SingletonDefaultCacheRequirement.SINGLETON_SERVICE_CONFIGURATOR_FACTORY.resolve(CONTAINER_NAME)));
        Consumer<ServiceTarget> installer = target -> {
            SingletonServiceConfiguratorFactory factory = factoryDependency.get();
            install(target, factory, SERVICE_A_NAME, SERVICE_A_PREFERRED_NODE);
            install(target, factory, SERVICE_B_NAME, SERVICE_B_PREFERRED_NODE);
        };
        builder.setInstance(new ChildTargetService(installer)).install();

        NodeServiceExecutorRegistry.INSTANCE.capture(SERVICE_A_NAME).install(context.getServiceTarget());
        NodeServiceExecutorRegistry.INSTANCE.capture(SERVICE_B_NAME).install(context.getServiceTarget());
    }

    private static void install(ServiceTarget target, SingletonServiceConfiguratorFactory factory, ServiceName name, String preferredNode) {
        ServiceBuilder<?> builder = target.addService(name);
        SingletonElectionListenerService listenerService = new SingletonElectionListenerService(builder.provides(name));
        builder.setInstance(listenerService).install();

        factory.createSingletonServiceConfigurator(name.append("singleton"))
                .electionPolicy(new PreferredSingletonElectionPolicy(new SimpleSingletonElectionPolicy(), new NamePreference(preferredNode)))
                .electionListener(listenerService)
                .build(target)
                .install();
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.Capability.KEY_AFFINITY_FACTORY;

import java.util.function.Consumer;

import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.controller.PathAddress;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.infinispan.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.infinispan.affinity.impl.DefaultKeyAffinityServiceFactory;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * Key affinity service factory that will only generates keys for use by the local node.
 * Returns a trivial implementation if the specified cache is not distributed.
 * @author Paul Ferraro
 */
public class KeyAffinityServiceFactoryServiceConfigurator extends CapabilityServiceNameProvider implements ServiceConfigurator {

    public KeyAffinityServiceFactoryServiceConfigurator(PathAddress address) {
        super(KEY_AFFINITY_FACTORY, address);
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<KeyAffinityServiceFactory> affinityFactory = builder.provides(name);
        Service service = Service.newInstance(affinityFactory, new DefaultKeyAffinityServiceFactory());
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}

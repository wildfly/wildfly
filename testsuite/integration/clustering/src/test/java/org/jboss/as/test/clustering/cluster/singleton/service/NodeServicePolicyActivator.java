/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.singleton.service;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.ChildTargetService;
import org.wildfly.clustering.singleton.SingletonDefaultRequirement;
import org.wildfly.clustering.singleton.service.SingletonPolicy;

/**
 * @author Paul Ferraro
 */
public class NodeServicePolicyActivator implements ServiceActivator {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("test", "service", "default-policy");

    @Override
    public void activate(ServiceActivatorContext context) {
        ServiceBuilder<?> builder = context.getServiceTarget().addService(ServiceName.JBOSS.append("test", "service", "installer"));
        Supplier<SingletonPolicy> policy = builder.requires(ServiceName.parse(SingletonDefaultRequirement.POLICY.getName()));
        Consumer<ServiceTarget> installer = target -> policy.get().createSingletonServiceConfigurator(SERVICE_NAME).build(target).install();
        builder.setInstance(new ChildTargetService(installer)).install();
    }
}

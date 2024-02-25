/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import static org.wildfly.extension.clustering.singleton.SingletonPolicyResourceDefinition.Capability.LEGACY_POLICY;

import java.util.EnumSet;

import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.singleton.Singleton;
import org.wildfly.clustering.singleton.SingletonRequirement;
import org.wildfly.extension.clustering.singleton.SingletonPolicyResourceDefinition.Capability;
import org.wildfly.service.capture.ServiceValueRegistry;

/**
 * @author Paul Ferraro
 */
@SuppressWarnings({ "removal", "deprecation" })
public class SingletonPolicyServiceHandler implements ResourceServiceHandler {

    private final ServiceValueRegistry<Singleton> registry;

    SingletonPolicyServiceHandler(ServiceValueRegistry<Singleton> registry) {
        this.registry = registry;
    }

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        ServiceTarget target = context.getServiceTarget();

        ServiceConfigurator configurator = new SingletonPolicyServiceConfigurator(address, this.registry).configure(context, model);
        configurator.build(target).install();

        // Use legacy service installation for legacy capability
        new AliasServiceBuilder<>(LEGACY_POLICY.getServiceName(address), configurator.getServiceName(), SingletonRequirement.SINGLETON_POLICY.getType()).build(target).install();
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        for (Capability capability : EnumSet.allOf(Capability.class)) {
            context.removeService(capability.getServiceName(address));
        }
    }
}

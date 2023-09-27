/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import static org.wildfly.extension.clustering.singleton.SingletonResourceDefinition.Attribute.DEFAULT;

import java.util.EnumSet;

import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.IdentityServiceConfigurator;
import org.wildfly.clustering.singleton.SingletonRequirement;
import org.wildfly.extension.clustering.singleton.SingletonResourceDefinition.Capability;

/**
 * @author Paul Ferraro
 */
@SuppressWarnings({ "removal", "deprecation" })
public class SingletonServiceHandler implements ResourceServiceHandler {

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        String defaultPolicy = DEFAULT.resolveModelAttribute(context, model).asString();
        ServiceTarget target = context.getServiceTarget();
        ServiceName serviceName = Capability.DEFAULT_POLICY.getServiceName(context.getCurrentAddress());
        ServiceName targetServiceName = SingletonServiceNameFactory.SINGLETON_POLICY.getServiceName(context, defaultPolicy);

        new IdentityServiceConfigurator<>(serviceName, targetServiceName).build(target).install();

        // Use legacy service installation for legacy capability
        ServiceName legacyServiceName = Capability.DEFAULT_LEGACY_POLICY.getServiceName(context.getCurrentAddress());

        new AliasServiceBuilder<>(legacyServiceName, targetServiceName, SingletonRequirement.SINGLETON_POLICY.getType()).build(target).install();
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) {
        PathAddress address = context.getCurrentAddress();
        for (Capability capability : EnumSet.allOf(Capability.class)) {
            context.removeService(capability.getServiceName(address));
        }
    }
}

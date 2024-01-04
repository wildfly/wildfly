/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.ejb;

import static org.wildfly.extension.clustering.ejb.DistributableEjbResourceDefinition.Attribute.DEFAULT_BEAN_MANAGEMENT;
import static org.wildfly.extension.clustering.ejb.DistributableEjbResourceDefinition.Capability.DEFAULT_BEAN_MANAGEMENT_PROVIDER;

import java.util.EnumSet;

import org.jboss.as.clustering.controller.IdentityCapabilityServiceConfigurator;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.ejb.bean.BeanProviderRequirement;
import org.wildfly.extension.clustering.ejb.DistributableEjbResourceDefinition.Capability;

/**
 * {@link ResourceServiceHandler} for the /subsystem=distributable-ejb resource.
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class DistributableEjbResourceServiceHandler implements ResourceServiceHandler {

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = DEFAULT_BEAN_MANAGEMENT.resolveModelAttribute(context, model).asString();
        new IdentityCapabilityServiceConfigurator<>(DEFAULT_BEAN_MANAGEMENT_PROVIDER.getServiceName(context.getCurrentAddress()), BeanProviderRequirement.BEAN_MANAGEMENT_PROVIDER, name)
                .configure(context)
                .build(context.getServiceTarget())
                .install();
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        for (Capability capability : EnumSet.allOf(Capability.class)) {
            context.removeService(capability.getServiceName(address));
        }
    }
}

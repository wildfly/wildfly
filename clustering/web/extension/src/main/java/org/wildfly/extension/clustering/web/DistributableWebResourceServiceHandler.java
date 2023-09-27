/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.EnumSet;

import org.jboss.as.clustering.controller.IdentityCapabilityServiceConfigurator;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.web.service.WebProviderRequirement;
import org.wildfly.extension.clustering.web.DistributableWebResourceDefinition.Attribute;
import org.wildfly.extension.clustering.web.DistributableWebResourceDefinition.Capability;

/**
 * {@link ResourceRuntimeHandler} for the /subsystem=distributable-web resource.
 * @author Paul Ferraro
 */
public class DistributableWebResourceServiceHandler implements ResourceServiceHandler {

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        ServiceTarget target = context.getServiceTarget();

        String defaultSessionManagement = Attribute.DEFAULT_SESSION_MANAGEMENT.resolveModelAttribute(context, model).asString();
        new IdentityCapabilityServiceConfigurator<>(Capability.DEFAULT_SESSION_MANAGEMENT_PROVIDER.getServiceName(address), WebProviderRequirement.SESSION_MANAGEMENT_PROVIDER, defaultSessionManagement)
                .configure(context)
                .build(target)
                .install();

        String defaultSSOManagement = Attribute.DEFAULT_SSO_MANAGEMENT.resolveModelAttribute(context, model).asString();
        new IdentityCapabilityServiceConfigurator<>(Capability.DEFAULT_SSO_MANAGEMENT_PROVIDER.getServiceName(address), WebProviderRequirement.SSO_MANAGEMENT_PROVIDER, defaultSSOManagement)
                .configure(context)
                .build(target)
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

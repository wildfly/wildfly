/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

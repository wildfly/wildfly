/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
import org.wildfly.clustering.ejb.EjbProviderRequirement;
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
        new IdentityCapabilityServiceConfigurator<>(DEFAULT_BEAN_MANAGEMENT_PROVIDER.getServiceName(context.getCurrentAddress()), EjbProviderRequirement.BEAN_MANAGEMENT_PROVIDER, name)
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

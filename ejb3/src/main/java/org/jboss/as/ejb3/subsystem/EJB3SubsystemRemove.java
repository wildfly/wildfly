/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition.CLUSTERED_SINGLETON_CAPABILITY;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition.EJB_CAPABILITY;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition.EJB_CLIENT_CONFIGURATOR;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Handler for removing the EJB3 subsystem.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class EJB3SubsystemRemove extends AbstractRemoveStepHandler {

    public static final EJB3SubsystemRemove INSTANCE = new EJB3SubsystemRemove();

    private EJB3SubsystemRemove() {
    }

    @Override
    protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        super.recordCapabilitiesAndRequirements(context, operation, resource);
        // TODO: delete these once optional requirements no longer require the existence of a capability
        context.deregisterCapability(CLUSTERED_SINGLETON_CAPABILITY.getName());
        context.deregisterCapability(EJB_CLIENT_CONFIGURATOR.getName());
        context.deregisterCapability(EJB_CAPABILITY.getName());

    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        // This subsystem registers DUPs, so we can't remove it from the runtime without a reload
        context.reloadRequired();
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        context.revertReloadRequired();
    }
}

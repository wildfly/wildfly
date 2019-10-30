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

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.ejb3.component.messagedriven.DefaultResourceAdapterService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;

/**
 * User: jpai
 */
public class DefaultResourceAdapterWriteHandler extends AbstractWriteAttributeHandler<Void> {

    public static final DefaultResourceAdapterWriteHandler INSTANCE = new DefaultResourceAdapterWriteHandler();

    private DefaultResourceAdapterWriteHandler() {
        super(EJB3SubsystemRootResourceDefinition.DEFAULT_RESOURCE_ADAPTER_NAME);
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                           ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
        final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        updateDefaultAdapterService(context, model);

        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                         ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        final ModelNode restored = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().clone();
        restored.get(attributeName).set(valueToRestore);
        updateDefaultAdapterService(context, restored);
    }

    void updateDefaultAdapterService(final OperationContext context, final ModelNode model) throws OperationFailedException {

        final ModelNode adapterNameNode = EJB3SubsystemRootResourceDefinition.DEFAULT_RESOURCE_ADAPTER_NAME.resolveModelAttribute(context, model);
        final String adapterName =  adapterNameNode.isDefined() ? adapterNameNode.asString() : null;

        final ServiceRegistry serviceRegistry = context.getServiceRegistry(true);
        ServiceController<DefaultResourceAdapterService> existingDefaultRANameService = (ServiceController<DefaultResourceAdapterService>) serviceRegistry.getService(DefaultResourceAdapterService.DEFAULT_RA_NAME_SERVICE_NAME);
        // if a default RA name service is already installed then just update the resource adapter name
        if (existingDefaultRANameService != null) {
            existingDefaultRANameService.getValue().setResourceAdapterName(adapterName);
        } else if (adapterName != null) {
            // create a new one and install
            final DefaultResourceAdapterService defaultResourceAdapterService = new DefaultResourceAdapterService(adapterName);
            ServiceController<?> newController =
                context.getServiceTarget().addService(DefaultResourceAdapterService.DEFAULT_RA_NAME_SERVICE_NAME, defaultResourceAdapterService)
                    .install();
        }

    }
}

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
package org.jboss.as.osgi.parser;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.osgi.framework.Services;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * @author David Bosschaert
 */
public class BundleRuntimeHandler extends AbstractRuntimeOnlyHandler {

    public static final BundleRuntimeHandler INSTANCE = new BundleRuntimeHandler();

    public static final String [] ATTRIBUTES = { CommonAttributes.ID, CommonAttributes.SYMBOLIC_NAME, CommonAttributes.VERSION };

    private BundleRuntimeHandler() {
    }

    public void register(ManagementResourceRegistration registry) {
        for (String attr : ATTRIBUTES) {
            registry.registerReadOnlyAttribute(attr, this, AttributeAccess.Storage.RUNTIME);
        }
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        final String operationName = operation.require(OP).asString();
        if (READ_ATTRIBUTE_OPERATION.equals(operationName)) {
            handleReadAttribute(context, operation);
        }
    }

    private void handleReadAttribute(OperationContext context, ModelNode operation) {
        String name = operation.require(ModelDescriptionConstants.NAME).asString();
        Long id = Long.parseLong(PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue());
        BundleContext bc = getBundleContext(context);

        if (CommonAttributes.ID.equals(name)) {
            context.getResult().set(id);
        } else if (CommonAttributes.SYMBOLIC_NAME.equals(name)) {
            Bundle b = bc.getBundle(id);
            context.getResult().set(b.getSymbolicName());
        } else if (CommonAttributes.VERSION.equals(name)) {
            Bundle b = bc.getBundle(id);
            context.getResult().set(b.getVersion().toString());
        }

        context.completeStep();
    }

    private BundleContext getBundleContext(OperationContext context) {
        ServiceController<?> sbs = context.getServiceRegistry(false).getService(Services.SYSTEM_BUNDLE);
        if (sbs == null) {
            return null;
        }

        Bundle systemBundle = Bundle.class.cast(sbs.getValue());
        return systemBundle.getBundleContext();
    }
}

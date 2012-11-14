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

package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ARCHIVE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.MODULE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;

import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Operation handler responsible for adding a Ra.
 *
 * @author maeste
 */
public class RaAdd extends AbstractAddStepHandler {
    static final RaAdd INSTANCE = new RaAdd();

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (final AttributeDefinition attribute : CommonAttributes.RESOURCE_ADAPTER_ATTRIBUTE) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    public void performRuntime(final OperationContext context, ModelNode operation, ModelNode model, final ServiceVerificationHandler verificationHandler,
                               final List<ServiceController<?>> controllers) throws OperationFailedException {
        // Compensating is remove
        final ModelNode address = operation.require(OP_ADDR);
        final String name = PathAddress.pathAddress(address).getLastElement().getValue();
        String archiveOrModuleName;
        if (model.get(ARCHIVE.getName()).isDefined()) {
            archiveOrModuleName = model.get(ARCHIVE.getName()).asString();
        } else {
            archiveOrModuleName = model.get(MODULE.getName()).asString();
        }

        if (name.startsWith(archiveOrModuleName) && (name.substring(archiveOrModuleName.length()).contains(ConnectorServices.RA_SERVICE_NAME_SEPARATOR) || name.equals(archiveOrModuleName))) {
            archiveOrModuleName = name;
        } else {
           Integer identifier = ConnectorServices.getResourceIdentifier(archiveOrModuleName);
            if (identifier != null && identifier != 0) {
                archiveOrModuleName = archiveOrModuleName + ConnectorServices.RA_SERVICE_NAME_SEPARATOR + identifier;
            }
        }
        ModifiableResourceAdapter resourceAdapter = RaOperationUtil.buildResourceAdaptersObject(context, operation, archiveOrModuleName);

        if (model.get(ARCHIVE.getName()).isDefined()) {
            model.get(ARCHIVE.getName()).set(archiveOrModuleName);
            RaOperationUtil.installRaServices(context, verificationHandler, name, resourceAdapter);
        } else {
            model.get(MODULE.getName()).set(archiveOrModuleName);
            RaOperationUtil.installRaServicesAndDeployFromModule(context, verificationHandler, name, resourceAdapter, archiveOrModuleName);
        }


    }
}

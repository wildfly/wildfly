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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

import java.util.List;

/**
 * Operation handler responsible for adding a Ra.
 *
 * @author maeste
 */
public class RaAdd extends AbstractAddStepHandler {
    static final RaAdd INSTANCE = new RaAdd();

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (final SimpleAttributeDefinition attribute : ResourceAdaptersSubsystemProviders.RESOURCEADAPTER_ATTRIBUTE) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    public void performRuntime(final OperationContext context, ModelNode operation, ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> controllers) throws OperationFailedException {

        // Compensating is remove
        final ModelNode address = operation.require(OP_ADDR);
        final String name = PathAddress.pathAddress(address).getLastElement().getValue();
        String archiveName =  model.get(ARCHIVE.getName()).asString();
        if (name.startsWith(archiveName) && (name.substring(archiveName.length()).contains(ConnectorServices.RA_SERVICE_NAME_SEPARATOR) || name.equals(archiveName))) {
            archiveName = name;
        } else {
           Integer identifier = ConnectorServices.getResourceIdentifier(archiveName);
            if (identifier != null && identifier != 0) {
                archiveName = archiveName + ConnectorServices.RA_SERVICE_NAME_SEPARATOR + identifier;
            }
        }
        model.get(ARCHIVE.getName()).set(archiveName);


        final ServiceTarget serviceTarget = context.getServiceTarget();

        ModifiableResourceAdapter resourceAdapter = RaOperationUtil.buildResourceAdaptersObject(context, operation);

        final ServiceController<?> resourceAdaptersService = context.getServiceRegistry(false).getService(
                ConnectorServices.RESOURCEADAPTERS_SERVICE);
        ServiceController<?> controller = null;
        if (resourceAdaptersService == null) {
            controller = serviceTarget.addService(ConnectorServices.RESOURCEADAPTERS_SERVICE,
                    new ResourceAdaptersService()).setInitialMode(Mode.ACTIVE).addListener(verificationHandler).install();
        }
        ServiceName raServiceName = ServiceName.of(ConnectorServices.RA_SERVICE, name);

        ResourceAdapterService raService = new ResourceAdapterService(resourceAdapter);
        serviceTarget.addService(raServiceName, raService).setInitialMode(Mode.ACTIVE)
                .addDependency(ConnectorServices.RESOURCEADAPTERS_SERVICE, ResourceAdaptersService.ModifiableResourceAdaptors.class, raService.getResourceAdaptersInjector())
                .addListener(verificationHandler).install();


    }
}

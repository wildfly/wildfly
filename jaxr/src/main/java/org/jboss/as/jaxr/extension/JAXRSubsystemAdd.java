/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jaxr.extension;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.jaxr.service.JAXRConnectionFactoryService;
import org.jboss.as.jaxr.service.JAXRDatasourceService;
import org.jboss.as.jaxr.service.JAXRConfiguration;
import org.jboss.as.jaxr.service.JUDDIContextService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.jaxr.extension.JAXRWriteAttributeHandler.applyUpdateToConfig;

/**
 * Handler responsible for adding the subsystem resource to the model
 *
 * @author Thomas.Diesler@jboss.com
 * @since 26-Oct-2011
 */
class JAXRSubsystemAdd extends AbstractAddStepHandler {

    static final JAXRSubsystemAdd INSTANCE = new JAXRSubsystemAdd();

    // Hide ctor
    private JAXRSubsystemAdd() {
    }

    static ModelNode createAddSubsystemOperation() {
        final ModelNode addop = new ModelNode();
        addop.get(OP).set(ADD);
        addop.get(OP_ADDR).add(SUBSYSTEM, JAXRConstants.SUBSYSTEM_NAME);
        return addop;
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (String attr : JAXRWriteAttributeHandler.REQUIRED_ATTRIBUTES) {
            ModelNode node = operation.get(attr);
            applyUpdateToConfig(attr, node);
            model.get(attr).set(node);
        }
    }

    @Override
    public void performRuntime(final OperationContext context, final ModelNode operation, ModelNode model, final ServiceVerificationHandler verifyHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                JAXRConfiguration config = JAXRConfiguration.INSTANCE;
                ServiceTarget serviceTarget = context.getServiceTarget();
                if (config.getConnectionFactoryBinding() != null) {
                    newControllers.add(JAXRConnectionFactoryService.addService(serviceTarget, config, verifyHandler));
                }
                // [TODO] AS7-2681 Make JAXR http endpoint configurable
                if (config.getDataSourceBinding() != null) {
                    newControllers.add(JAXRDatasourceService.addService(serviceTarget, config, verifyHandler));
                    newControllers.add(JUDDIContextService.addService(serviceTarget, config, verifyHandler));
                }
                context.completeStep();
            }
        }, OperationContext.Stage.RUNTIME);
    }
}

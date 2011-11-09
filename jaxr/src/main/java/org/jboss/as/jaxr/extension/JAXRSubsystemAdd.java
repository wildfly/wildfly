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
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.jaxr.service.JAXRBootstrapService;
import org.jboss.as.jaxr.service.JAXRConfiguration;
import org.jboss.as.jaxr.service.JUDDIContextService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

import java.util.List;
import java.util.Locale;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

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
        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).add(SUBSYSTEM, JAXRConstants.SUBSYSTEM_NAME);
        return subsystem;
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        JAXRConfiguration config = JAXRConfiguration.INSTANCE;
        if (operation.has(ModelConstants.CONNECTIONFACTORY)) {
            ModelNode node = operation.get(ModelConstants.CONNECTIONFACTORY);
            model.get(ModelConstants.CONNECTIONFACTORY).set(node);
            config.setConnectionFactoryBinding(node.asString());
        }
        if (operation.has(ModelConstants.DATASOURCE)) {
            ModelNode node = operation.get(ModelConstants.DATASOURCE);
            model.get(ModelConstants.DATASOURCE).set(node);
            config.setDataSourceBinding(node.asString());
        }
        if (operation.has(ModelConstants.DROPONSTART)) {
            ModelNode node = operation.get(ModelConstants.DROPONSTART);
            model.get(ModelConstants.DROPONSTART).set(node);
            config.setDropOnStart(node.asBoolean());
        }
        if (operation.has(ModelConstants.CREATEONSTART)) {
            ModelNode node = operation.get(ModelConstants.CREATEONSTART);
            model.get(ModelConstants.CREATEONSTART).set(node);
            config.setCreateOnStart(node.asBoolean());
        }
        if (operation.has(ModelConstants.DROPONSTOP)) {
            ModelNode node = operation.get(ModelConstants.DROPONSTOP);
            model.get(ModelConstants.DROPONSTOP).set(node);
            config.setDropOnStop(node.asBoolean());
        }
    }

    @Override
    public void performRuntime(final OperationContext context, final ModelNode operation, ModelNode model, final ServiceVerificationHandler verifyHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                JAXRConfiguration config = JAXRConfiguration.INSTANCE;
                ServiceTarget serviceTarget = context.getServiceTarget();
                newControllers.add(JAXRBootstrapService.addService(serviceTarget, config, verifyHandler));
                newControllers.add(JUDDIContextService.addService(serviceTarget, config, verifyHandler));
                context.completeStep();
            }
        }, OperationContext.Stage.RUNTIME);
    }

    /**
     * Used to create the description of the subsystem add method
     */
    static DescriptionProvider DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            final ModelNode node = new ModelNode();
            node.get(ModelDescriptionConstants.OPERATION_NAME).set(ModelDescriptionConstants.ADD);
            node.get(ModelDescriptionConstants.DESCRIPTION).set("Adds the JAXR subsystem");
            node.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelConstants.CONNECTIONFACTORY, ModelDescriptionConstants.DESCRIPTION).set("The JNDI name for the ConnectionFactory");
            node.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelConstants.CONNECTIONFACTORY, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
            node.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelConstants.CONNECTIONFACTORY, ModelDescriptionConstants.REQUIRED).set(false);
            node.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelConstants.DATASOURCE, ModelDescriptionConstants.DESCRIPTION).set("The JNDI name for the DataSource");
            node.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelConstants.DATASOURCE, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
            node.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelConstants.DATASOURCE, ModelDescriptionConstants.REQUIRED).set(false);
            node.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelConstants.DROPONSTART, ModelDescriptionConstants.DESCRIPTION).set("Should tables be dropped on Start");
            node.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelConstants.DROPONSTART, ModelDescriptionConstants.TYPE).set(ModelType.BOOLEAN);
            node.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelConstants.DROPONSTART, ModelDescriptionConstants.REQUIRED).set(false);
            node.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelConstants.CREATEONSTART, ModelDescriptionConstants.DESCRIPTION).set("Should tables be dropped on Start");
            node.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelConstants.CREATEONSTART, ModelDescriptionConstants.TYPE).set(ModelType.BOOLEAN);
            node.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelConstants.CREATEONSTART, ModelDescriptionConstants.REQUIRED).set(false);
            node.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelConstants.DROPONSTOP, ModelDescriptionConstants.DESCRIPTION).set("Should tables be dropped on Start");
            node.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelConstants.DROPONSTOP, ModelDescriptionConstants.TYPE).set(ModelType.BOOLEAN);
            node.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelConstants.DROPONSTOP, ModelDescriptionConstants.REQUIRED).set(false);
            node.get(ModelDescriptionConstants.REPLY_PROPERTIES).setEmptyObject();
            return node;
        }
    };
}

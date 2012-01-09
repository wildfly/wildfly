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
package org.jboss.as.configadmin.parser;

import org.jboss.as.configadmin.service.ConfigAdminServiceImpl;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.jboss.as.configadmin.ConfigAdminLogger.ROOT_LOGGER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

/**
 * ConfigAdmin subsystem operation handler.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 11-Sep-2010
 */
class ConfigAdminAdd extends AbstractBoottimeAddStepHandler {

    static final ConfigAdminAdd INSTANCE = new ConfigAdminAdd();

    private ConfigAdminAdd() {
        // Private to ensure a singleton.
    }

    static ModelNode createAddSubsystemOperation() {
        final ModelNode addop = new ModelNode();
        addop.get(OP).set(ADD);
        addop.get(OP_ADDR).add(SUBSYSTEM, ConfigAdminExtension.SUBSYSTEM_NAME);
        return addop;
    }

    protected void populateModel(final ModelNode operation, final ModelNode model) {
    }

    protected void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model,
            final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) {

        ROOT_LOGGER.activatingSubsystem();

        context.addStep(new OperationStepHandler() {
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                ServiceTarget serviceTarget = context.getServiceTarget();
                newControllers.add(ConfigAdminServiceImpl.addService(serviceTarget, verificationHandler));
                context.completeStep();
            }
        }, OperationContext.Stage.RUNTIME);


        ServiceTarget serviceTarget = context.getServiceTarget();
        newControllers.add(ConfigAdminState.addService(serviceTarget));


        ROOT_LOGGER.debugf("Activated ConfigAdmin Subsystem");
    }

    static DescriptionProvider DESCRIPTION = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(Locale locale) {
            ModelNode node = new ModelNode();
            ResourceBundle resbundle = ConfigAdminProviders.getResourceBundle(locale);
            node.get(ModelDescriptionConstants.OPERATION_NAME).set(ModelDescriptionConstants.ADD);
            node.get(ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("subsystem.add"));
            node.get(ModelDescriptionConstants.REPLY_PROPERTIES).setEmptyObject();
            return node;
        }
    };
}

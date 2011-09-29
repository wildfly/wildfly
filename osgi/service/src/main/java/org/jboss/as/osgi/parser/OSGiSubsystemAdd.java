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

import static org.jboss.as.osgi.OSGiLogger.ROOT_LOGGER;
import static org.jboss.as.osgi.parser.CommonAttributes.ACTIVATION;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.osgi.deployment.BundleStartTracker;
import org.jboss.as.osgi.deployment.OSGiDeploymentActivator;
import org.jboss.as.osgi.parser.SubsystemState.Activation;
import org.jboss.as.osgi.service.BundleInstallProviderIntegration;
import org.jboss.as.osgi.service.ConfigAdminServiceImpl;
import org.jboss.as.osgi.service.FrameworkBootstrapService;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * OSGi subsystem operation handler.
 *
 * @author Thomas.Diesler@jboss.com
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author David Bosschaert
 * @since 11-Sep-2010
 */
class OSGiSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final OSGiSubsystemAdd INSTANCE = new OSGiSubsystemAdd();

    private OSGiSubsystemAdd() {
        // Private to ensure a singleton.
    }

    protected void populateModel(final ModelNode operation, final ModelNode subModel) {
        if (operation.has(ACTIVATION)) {
            subModel.get(ACTIVATION).set(operation.get(ACTIVATION));
        }
    }

    protected void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model,
            final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) {

        ROOT_LOGGER.activatingSubsystem();

        context.addStep(new OperationStepHandler() {
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                ServiceTarget serviceTarget = context.getServiceTarget();
                newControllers.add(BundleStartTracker.addService(serviceTarget));
                newControllers.add(BundleInstallProviderIntegration.addService(serviceTarget));
                newControllers.addAll(FrameworkBootstrapService.addService(serviceTarget, verificationHandler));
                newControllers.add(ConfigAdminServiceImpl.addService(serviceTarget, verificationHandler));
                context.completeStep();
            }
        }, OperationContext.Stage.RUNTIME);

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                new OSGiDeploymentActivator().activate(processorTarget);
            }
        }, OperationContext.Stage.RUNTIME);

        ServiceTarget serviceTarget = context.getServiceTarget();
        newControllers.add(SubsystemState.addService(serviceTarget, getActivationMode(operation)));

        ROOT_LOGGER.debugf("Activated OSGi Subsystem");
    }

    private Activation getActivationMode(ModelNode operation) {
        Activation activation = SubsystemState.DEFAULT_ACTIVATION;
        if (operation.has(ACTIVATION)) {
            activation = Activation.valueOf(operation.get(ACTIVATION).asString().toUpperCase());
        }
        return activation;
    }

    static DescriptionProvider DESCRIPTION = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(Locale locale) {
            ModelNode node = new ModelNode();
            ResourceBundle resbundle = OSGiSubsystemProviders.getResourceBundle(locale);
            node.get(ModelDescriptionConstants.OPERATION_NAME).set(ModelDescriptionConstants.ADD);
            node.get(ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("subsystem.add"));
            node.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.ACTIVATION, ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("subsystem.activation"));
            node.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.ACTIVATION, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
            node.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.ACTIVATION, ModelDescriptionConstants.DEFAULT).set(SubsystemState.DEFAULT_ACTIVATION.toString());
            node.get(ModelDescriptionConstants.REPLY_PROPERTIES).setEmptyObject();
            return node;
        }
    };
}

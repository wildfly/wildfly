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

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.osgi.deployment.BundleStartTracker;
import org.jboss.as.osgi.deployment.OSGiDeploymentActivator;
import org.jboss.as.osgi.parser.SubsystemState.Activation;
import org.jboss.as.osgi.service.BundleInstallProviderIntegration;
import org.jboss.as.osgi.service.FrameworkBootstrapService;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.osgi.framework.Services;
import org.osgi.framework.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.jboss.as.osgi.OSGiLogger.ROOT_LOGGER;

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

    private OSGiRuntimeResource resource;

    private OSGiSubsystemAdd() {
        // Private to ensure a singleton.
    }

    // Can't use the execute() from the base class here because we use a custom resource.
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        resource = new OSGiRuntimeResource();
        context.addResource(PathAddress.EMPTY_ADDRESS, resource);

        // rest of super.execute() method is unchanged below
        populateModel(operation, resource);
        final ModelNode model = resource.getModel();

        if (requiresRuntime(context)) {
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    final List<ServiceController<?>> controllers = new ArrayList<ServiceController<?>>();
                    final ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
                    performRuntime(context, operation, model, verificationHandler, controllers);

                    if(requiresRuntimeVerification()) {
                        context.addStep(verificationHandler, OperationContext.Stage.VERIFY);
                    }

                    if (context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                        rollbackRuntime(context, operation, model, controllers);
                    }
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.completeStep();
    }

    protected void populateModel(final ModelNode operation, final ModelNode model) {
        if (operation.has(ModelConstants.ACTIVATION)) {
            model.get(ModelConstants.ACTIVATION).set(operation.get(ModelConstants.ACTIVATION));
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
                newControllers.add(FrameworkBootstrapService.addService(serviceTarget, verificationHandler));
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

        // This step injects the System Bundle Service into our custom resource
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                ServiceBuilder<Void> builder = context.getServiceTarget().addService(
                        Services.JBOSGI_BASE_NAME.append("OSGiSubsystem").append("initialize"),
                    new AbstractService<Void>() {
                        @SuppressWarnings("unchecked")
                        @Override
                        public void start(StartContext context) throws StartException {
                            try {
                                ServiceContainer ctr = context.getController().getServiceContainer();
                                ServiceController<Bundle> sc = (ServiceController<Bundle>) ctr.getRequiredService(Services.SYSTEM_BUNDLE);
                                resource.setBundleContextServiceController(sc);
                            } finally {
                                context.getController().setMode(Mode.REMOVE);
                            }
                        }
                    });
                builder.addDependency(Services.SYSTEM_BUNDLE);
                builder.setInitialMode(Mode.PASSIVE);
                builder.install();
                context.completeStep();
            }
        }, OperationContext.Stage.RUNTIME);

        ROOT_LOGGER.debugf("Activated OSGi Subsystem");
    }

    private Activation getActivationMode(ModelNode operation) {
        Activation activation = SubsystemState.DEFAULT_ACTIVATION;
        if (operation.has(ModelConstants.ACTIVATION)) {
            activation = Activation.valueOf(operation.get(ModelConstants.ACTIVATION).asString().toUpperCase());
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
            node.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelConstants.ACTIVATION, ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("subsystem.activation"));
            node.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelConstants.ACTIVATION, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
            node.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelConstants.ACTIVATION, ModelDescriptionConstants.DEFAULT).set(SubsystemState.DEFAULT_ACTIVATION.toString());
            node.get(ModelDescriptionConstants.REPLY_PROPERTIES).setEmptyObject();
            return node;
        }
    };
}

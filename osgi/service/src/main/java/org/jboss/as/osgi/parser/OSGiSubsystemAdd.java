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

import static org.jboss.as.osgi.OSGiLogger.LOGGER;

import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.osgi.deployment.BundleContextBindingProcessor;
import org.jboss.as.osgi.deployment.BundleDeploymentProcessor;
import org.jboss.as.osgi.deployment.BundleInstallProcessor;
import org.jboss.as.osgi.deployment.ModuleRegisterProcessor;
import org.jboss.as.osgi.deployment.OSGiBundleInfoParseProcessor;
import org.jboss.as.osgi.deployment.OSGiManifestStructureProcessor;
import org.jboss.as.osgi.deployment.OSGiXServiceParseProcessor;
import org.jboss.as.osgi.management.OSGiRuntimeResource;
import org.jboss.as.osgi.parser.SubsystemState.Activation;
import org.jboss.as.osgi.service.FrameworkBootstrapService;
import org.jboss.as.osgi.service.PersistentBundlesIntegration;
import org.jboss.as.osgi.service.PersistentBundlesIntegration.InitialDeploymentTracker;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
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

    private OSGiRuntimeResource resource;

    private OSGiSubsystemAdd() {
    }

    @Override
    protected Resource createResource(OperationContext context) {
        resource = new OSGiRuntimeResource();
        context.addResource(PathAddress.EMPTY_ADDRESS, resource);
        return resource;
    }

    protected void populateModel(final ModelNode operation, final ModelNode model) {
        if (operation.has(ModelConstants.ACTIVATION)) {
            model.get(ModelConstants.ACTIVATION).set(operation.get(ModelConstants.ACTIVATION));
        }
    }

    protected void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model,
            final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) {

        LOGGER.infoActivatingSubsystem();

        final ServiceTarget serviceTarget = context.getServiceTarget();
        final Activation activationMode = getActivationMode(operation);
        final InitialDeploymentTracker deploymentTracker = new InitialDeploymentTracker(context, activationMode);

        context.addStep(new OperationStepHandler() {
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                newControllers.add(PersistentBundlesIntegration.addService(serviceTarget, deploymentTracker));
                newControllers.add(FrameworkBootstrapService.addService(serviceTarget, resource, verificationHandler));
                context.completeStep();
            }
        }, OperationContext.Stage.RUNTIME);

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(OSGiExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_OSGI_MANIFEST, new OSGiManifestStructureProcessor());
                processorTarget.addDeploymentProcessor(OSGiExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_OSGI_BUNDLE_INFO, new OSGiBundleInfoParseProcessor());
                processorTarget.addDeploymentProcessor(OSGiExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_OSGI_XSERVICE_PROPERTIES, new OSGiXServiceParseProcessor());
                processorTarget.addDeploymentProcessor(OSGiExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_OSGI_DEPLOYMENT, new BundleDeploymentProcessor());
                processorTarget.addDeploymentProcessor(OSGiExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_BUNDLE_CONTEXT_BINDING, new BundleContextBindingProcessor());
                processorTarget.addDeploymentProcessor(OSGiExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_OSGI_DEPLOYMENT, new BundleInstallProcessor(deploymentTracker));
                processorTarget.addDeploymentProcessor(OSGiExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_OSGI_MODULE, new ModuleRegisterProcessor());
            }
        }, OperationContext.Stage.RUNTIME);

        // Add the subsystem state as a service
        newControllers.add(SubsystemState.addService(serviceTarget, activationMode));
    }

    private Activation getActivationMode(ModelNode operation) {
        Activation activation = SubsystemState.DEFAULT_ACTIVATION;
        if (operation.has(ModelConstants.ACTIVATION)) {
            activation = Activation.valueOf(operation.get(ModelConstants.ACTIVATION).asString().toUpperCase(Locale.ENGLISH));
        }
        return activation;
    }

}

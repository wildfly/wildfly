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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ServiceLoader;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.osgi.SubsystemExtension;
import org.jboss.as.osgi.deployment.BundleActivateProcessor;
import org.jboss.as.osgi.deployment.BundleDeploymentProcessor;
import org.jboss.as.osgi.deployment.BundleInstallProcessor;
import org.jboss.as.osgi.deployment.BundleResolveProcessor;
import org.jboss.as.osgi.deployment.BundleSubDeploymentMarkingProcessor;
import org.jboss.as.osgi.deployment.FrameworkActivateProcessor;
import org.jboss.as.osgi.deployment.ModuleRegisterProcessor;
import org.jboss.as.osgi.deployment.OSGiBundleInfoParseProcessor;
import org.jboss.as.osgi.deployment.OSGiComponentParseProcessor;
import org.jboss.as.osgi.deployment.OSGiManifestStructureProcessor;
import org.jboss.as.osgi.deployment.OSGiXServiceParseProcessor;
import org.jboss.as.osgi.management.OSGiRuntimeResource;
import org.jboss.as.osgi.parser.SubsystemState.Activation;
import org.jboss.as.osgi.service.FrameworkActivator;
import org.jboss.as.osgi.service.FrameworkBootstrapService;
import org.jboss.as.osgi.service.InitialDeploymentTracker;
import org.jboss.as.osgi.service.ModuleRegistrationTracker;
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

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) {
        if (operation.has(ModelConstants.ACTIVATION)) {
            model.get(ModelConstants.ACTIVATION).set(operation.get(ModelConstants.ACTIVATION));
        }
    }

    @Override
    protected void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model,
            final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) {

        LOGGER.infoActivatingSubsystem();

        final Activation activation = getActivationMode(operation);
        final ServiceTarget serviceTarget = context.getServiceTarget();
        final InitialDeploymentTracker deploymentTracker = new InitialDeploymentTracker(context, verificationHandler);
        final ModuleRegistrationTracker registrationTracker = new ModuleRegistrationTracker();

        // Collect the subsystem extensions
        final List<SubsystemExtension> extensions = new ArrayList<SubsystemExtension>();
        final Iterator<SubsystemExtension> services = ServiceLoader.load(SubsystemExtension.class, getClass().getClassLoader()).iterator();
        while(services.hasNext()) {
            extensions.add(services.next());
        }

        // Create the framework activator
        FrameworkActivator.create(serviceTarget, activation == Activation.LAZY);

        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                newControllers.add(FrameworkBootstrapService.addService(serviceTarget, resource, extensions, verificationHandler));
                newControllers.add(registrationTracker.install(serviceTarget, verificationHandler));
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            }
        }, OperationContext.Stage.RUNTIME);

        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            protected void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(OSGiExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_OSGI_MANIFEST, new OSGiManifestStructureProcessor());
                processorTarget.addDeploymentProcessor(OSGiExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_BUNDLE_SUB_DEPLOYMENT, new BundleSubDeploymentMarkingProcessor());
                processorTarget.addDeploymentProcessor(OSGiExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_OSGI_BUNDLE_INFO, new OSGiBundleInfoParseProcessor());
                processorTarget.addDeploymentProcessor(OSGiExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_OSGI_PROPERTIES, new OSGiXServiceParseProcessor());
                processorTarget.addDeploymentProcessor(OSGiExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_OSGI_DEPLOYMENT, new BundleDeploymentProcessor());
                processorTarget.addDeploymentProcessor(OSGiExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_OSGI_COMPONENTS, new OSGiComponentParseProcessor());
                processorTarget.addDeploymentProcessor(OSGiExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_OSGI_SUBSYSTEM_ACTIVATOR, new FrameworkActivateProcessor(deploymentTracker));
                processorTarget.addDeploymentProcessor(OSGiExtension.SUBSYSTEM_NAME, Phase.REGISTER, Phase.REGISTER_BUNDLE_INSTALL, new BundleInstallProcessor(deploymentTracker));
                processorTarget.addDeploymentProcessor(OSGiExtension.SUBSYSTEM_NAME, Phase.CONFIGURE_MODULE, Phase.CONFIGURE_RESOLVE_BUNDLE, new BundleResolveProcessor());
                processorTarget.addDeploymentProcessor(OSGiExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_RESOLVER_MODULE, new ModuleRegisterProcessor(registrationTracker));
                processorTarget.addDeploymentProcessor(OSGiExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_BUNDLE_ACTIVATE, new BundleActivateProcessor());
            }
        }, OperationContext.Stage.RUNTIME);

        // Perform boottime on subsystem extensions
        for(SubsystemExtension extension : extensions) {
            extension.performBoottime(context, operation, model, verificationHandler, newControllers);
        }

        // Add the subsystem state as a service
        newControllers.add(SubsystemState.addService(serviceTarget, activation));
    }

    private Activation getActivationMode(ModelNode operation) {
        Activation activation = SubsystemState.DEFAULT_ACTIVATION;
        if (operation.has(ModelConstants.ACTIVATION)) {
            activation = Activation.valueOf(operation.get(ModelConstants.ACTIVATION).asString().toUpperCase(Locale.ENGLISH));
        }
        return activation;
    }

}

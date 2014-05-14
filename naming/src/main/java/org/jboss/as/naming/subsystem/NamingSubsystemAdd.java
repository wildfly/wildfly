/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.naming.subsystem;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.naming.NamingContext;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.context.external.ExternalContexts;
import org.jboss.as.naming.context.external.ExternalContextsNavigableSet;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.ExternalContextsProcessor;
import org.jboss.as.naming.deployment.JndiNamingDependencyProcessor;
import org.jboss.as.naming.management.JndiViewExtensionRegistry;
import org.jboss.as.naming.service.DefaultNamespaceContextSelectorService;
import org.jboss.as.naming.service.ExternalContextsService;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.naming.service.NamingStoreService;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

import java.util.List;

import static org.jboss.as.naming.logging.NamingLogger.ROOT_LOGGER;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 * @author John Bailey
 * @author Eduardo Martins
 */
public class NamingSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final NamingSubsystemAdd INSTANCE = new NamingSubsystemAdd();

    protected void populateModel(ModelNode operation, ModelNode model) {
        model.setEmptyObject();
    }

    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {

        ROOT_LOGGER.activatingSubsystem();

        NamingContext.initializeNamingManager();
        final ServiceTarget target = context.getServiceTarget();

        // Create the java: namespace
        newControllers.add(target.addService(ContextNames.JAVA_CONTEXT_SERVICE_NAME, new NamingStoreService())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .addListener(verificationHandler)
                .install());

        // Create the Naming Service
        final NamingService namingService = new NamingService();
        newControllers.add(target.addService(NamingService.SERVICE_NAME, namingService)
                .addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME, NamingStore.class, namingService.getNamingStore())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .addListener(verificationHandler)
                .install());

        // Create the java:global namespace
        newControllers.add(target.addService(ContextNames.GLOBAL_CONTEXT_SERVICE_NAME, new NamingStoreService())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .addListener(verificationHandler)
                .install());

        // Create the java:jboss vendor namespace
        newControllers.add(target.addService(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, new NamingStoreService())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .addListener(verificationHandler)
                .install());

        // Setup remote naming store
        //we always install the naming store, but we don't install the server unless it has been explicitly enabled
        newControllers.add(target.addService(ContextNames.EXPORTED_CONTEXT_SERVICE_NAME, new NamingStoreService())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .addListener(verificationHandler)
                .install());

        // add the default namespace context selector service
        DefaultNamespaceContextSelectorService defaultNamespaceContextSelectorService = new DefaultNamespaceContextSelectorService();
        newControllers.add(target.addService(DefaultNamespaceContextSelectorService.SERVICE_NAME, defaultNamespaceContextSelectorService)
                .addDependency(ContextNames.GLOBAL_CONTEXT_SERVICE_NAME, NamingStore.class, defaultNamespaceContextSelectorService.getGlobalNamingStore())
                .addDependency(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, NamingStore.class, defaultNamespaceContextSelectorService.getJbossNamingStore())
                .addDependency(ContextNames.EXPORTED_CONTEXT_SERVICE_NAME, NamingStore.class, defaultNamespaceContextSelectorService.getRemoteExposedNamingStore())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .addListener(verificationHandler)
                .install());

        newControllers.add(target.addService(JndiViewExtensionRegistry.SERVICE_NAME, new JndiViewExtensionRegistry()).install());

        // create the subsystem's external context instance, and install related Service and DUP
        final ExternalContexts externalContexts = new ExternalContextsNavigableSet();
        newControllers.add(target.addService(ExternalContextsService.SERVICE_NAME, new ExternalContextsService(externalContexts)).install());

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(NamingExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_NAMING_EXTERNAL_CONTEXTS, new ExternalContextsProcessor(externalContexts));
                processorTarget.addDeploymentProcessor(NamingExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_JNDI_DEPENDENCIES, new JndiNamingDependencyProcessor());
            }
        }, OperationContext.Stage.RUNTIME);


    }
}

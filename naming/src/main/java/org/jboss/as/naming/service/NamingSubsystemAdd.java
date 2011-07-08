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

package org.jboss.as.naming.service;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.naming.InMemoryNamingStore;
import org.jboss.as.naming.InitialContextFactoryService;
import org.jboss.as.naming.NamingContext;
import org.jboss.as.naming.NamingEventCoordinator;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiNamingDependencyProcessor;
import org.jboss.as.naming.deployment.JndiNamingDependencySetupProcessor;
import org.jboss.as.naming.management.JndiViewExtensionRegistry;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

import javax.management.MBeanServer;
import java.util.List;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 */
public class NamingSubsystemAdd extends AbstractAddStepHandler {

    private static final Logger log = Logger.getLogger("org.jboss.as.naming");

    static final NamingSubsystemAdd INSTANCE = new NamingSubsystemAdd();

    protected void populateModel(ModelNode operation, ModelNode model) {
        model.setEmptyObject();
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {

        log.info("Activating Naming Subsystem");

        NamingContext.initializeNamingManager();

        final NamingStore namingStore = new InMemoryNamingStore(new NamingEventCoordinator());

        // Create the Naming Service
        final ServiceTarget target = context.getServiceTarget();
        newControllers.add(target.addService(NamingService.SERVICE_NAME, new NamingService(namingStore))
                .addAliases(ContextNames.JAVA_CONTEXT_SERVICE_NAME)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .addListener(verificationHandler)
                .install());

        // Create the java:global namespace
        newControllers.add(addGlobalContextFactory(target, "global", verificationHandler));
        // Create the java:jboss vendor namespace
        newControllers.add(addGlobalContextFactory(target, "jboss", verificationHandler));

        // Create the EE namespace
        newControllers.add(addContextFactory(target, "app", verificationHandler));
        newControllers.add(addContextFactory(target, "module", verificationHandler));
        newControllers.add(addContextFactory(target, "comp", verificationHandler));

        // Provide the {@link InitialContext} as OSGi service
        newControllers.add(InitialContextFactoryService.addService(target, verificationHandler));

        newControllers.add(target.addService(JndiViewExtensionRegistry.SERVICE_NAME, new JndiViewExtensionRegistry()).install());

        if (context.isBooting()) {
            context.addStep(new AbstractDeploymentChainStep() {
                        protected void execute(DeploymentProcessorTarget processorTarget) {

                            processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_JNDI_DEPENDENCY_SETUP, new JndiNamingDependencySetupProcessor());
                            processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_JNDI_DEPENDENCIES, new JndiNamingDependencyProcessor());
                        }
                    }, OperationContext.Stage.RUNTIME);
        }

    }

    private static ServiceController<?> addContextFactory(final ServiceTarget target, final String contextName, final ServiceVerificationHandler verificationHandler) {
        final EEContextService eeContextService = new EEContextService(contextName);
        return target.addService(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(contextName), eeContextService)
                .addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME, NamingStore.class, eeContextService.getJavaContextInjector())
                .addListener(verificationHandler)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }

    private static ServiceController<?> addGlobalContextFactory(final ServiceTarget target, final String contextName, final ServiceVerificationHandler verificationHandler) {
        final GlobalContextService eeContextService = new GlobalContextService(contextName);
        return target.addService(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(contextName), eeContextService)
                .addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME, NamingStore.class, eeContextService.getJavaContextInjector())
                .addListener(verificationHandler)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }
}

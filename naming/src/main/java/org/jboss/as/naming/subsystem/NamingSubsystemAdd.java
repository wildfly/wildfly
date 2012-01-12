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

import static org.jboss.as.naming.NamingLogger.ROOT_LOGGER;

import java.util.List;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.naming.InitialContextFactoryBuilder;
import org.jboss.as.naming.NamingContext;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.WritableServiceBasedNamingStore;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiNamingDependencyProcessor;
import org.jboss.as.naming.deployment.JndiNamingDependencySetupProcessor;
import org.jboss.as.naming.management.JndiViewExtensionRegistry;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.naming.service.NamingStoreService;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 * @author John Bailey
 */
public class NamingSubsystemAdd extends AbstractBoottimeAddStepHandler {
    private static final CompositeName EMPTY_NAME = new CompositeName();

    static final NamingSubsystemAdd INSTANCE = new NamingSubsystemAdd();

    protected void populateModel(ModelNode operation, ModelNode model) {
        model.setEmptyObject();
    }

    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {

        ROOT_LOGGER.activatingSubsystem();

        NamingContext.initializeNamingManager();

        final ServiceBasedNamingStore namingStore = new WritableServiceBasedNamingStore(context.getServiceRegistry(false), ContextNames.JAVA_CONTEXT_SERVICE_NAME);

        // Create the Naming Service
        final ServiceTarget target = context.getServiceTarget();
        newControllers.add(target.addService(NamingService.SERVICE_NAME, new NamingService(namingStore))
                .addAliases(ContextNames.JAVA_CONTEXT_SERVICE_NAME)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .addListener(verificationHandler)
                .install());

        // Create the java:global namespace
        final ServiceBasedNamingStore globalNamingStore = new WritableServiceBasedNamingStore(context.getServiceRegistry(false), ContextNames.GLOBAL_CONTEXT_SERVICE_NAME);
        newControllers.add(target.addService(ContextNames.GLOBAL_CONTEXT_SERVICE_NAME, new NamingStoreService(globalNamingStore))
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .addListener(verificationHandler)
                .install());

        // Create the java:jboss vendor namespace
        final ServiceBasedNamingStore jbossNamingStore = new WritableServiceBasedNamingStore(context.getServiceRegistry(false), ContextNames.JBOSS_CONTEXT_SERVICE_NAME);
        newControllers.add(target.addService(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, new NamingStoreService(jbossNamingStore))
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .addListener(verificationHandler)
                .install());

        NamespaceContextSelector.setDefault(new NamespaceContextSelector() {
            public Context getContext(String identifier) {
                final NamingStore namingStore;
                if(identifier.equals("global")){
                    namingStore = globalNamingStore;
                } else if(identifier.equals("jboss")) {
                    namingStore = jbossNamingStore;
                } else {
                    namingStore = null;
                }
                if (namingStore != null) {
                    try {
                        return (Context) namingStore.lookup(EMPTY_NAME);
                    } catch (NamingException e) {
                        throw new IllegalStateException(e);
                    }
                } else {
                    return null;
                }
            }
        });

        // Register InitialContext and InitialContextFactoryBuilder as OSGi services
        newControllers.add(NamingSubsystemOSGiService.addService(target,
                InitialContext.class, InitialContext.class, verificationHandler));
        newControllers.add(NamingSubsystemOSGiService.addService(target,
                javax.naming.spi.InitialContextFactoryBuilder.class, InitialContextFactoryBuilder.class, verificationHandler));

        newControllers.add(target.addService(JndiViewExtensionRegistry.SERVICE_NAME, new JndiViewExtensionRegistry()).install());

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_JNDI_DEPENDENCY_SETUP, new JndiNamingDependencySetupProcessor());
                processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_JNDI_DEPENDENCIES, new JndiNamingDependencyProcessor());
            }
        }, OperationContext.Stage.RUNTIME);

    }
}

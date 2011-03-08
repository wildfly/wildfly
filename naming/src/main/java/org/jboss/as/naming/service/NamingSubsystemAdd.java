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

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.NamingException;
import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import javax.management.MBeanServer;

import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.naming.InMemoryNamingStore;
import org.jboss.as.naming.InitialContextFactoryService;
import org.jboss.as.naming.NamingContext;
import org.jboss.as.naming.NamingEventCoordinator;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.context.ObjectFactoryBuilder;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.server.BootOperationContext;
import org.jboss.as.server.BootOperationHandler;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 */
public class NamingSubsystemAdd implements ModelAddOperationHandler, BootOperationHandler {

    private static final Logger log = Logger.getLogger("org.jboss.as.naming");

    static final NamingSubsystemAdd INSTANCE = new NamingSubsystemAdd();

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

        context.getSubModel().setEmptyObject();

        if (context instanceof BootOperationContext) {
            final BootOperationContext updateContext = (BootOperationContext) context;

            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    updateContext.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_NAMING, new NamingDependencyProcessor());

                    log.info("Activating Naming Subsystem");

                    ObjectFactoryBuilder.INSTANCE.setServiceRegistry(context.getServiceRegistry());

                    NamingContext.initializeNamingManager();

                    final NamingStore namingStore = new InMemoryNamingStore(new NamingEventCoordinator());

                    // Create the Naming Service
                    final ServiceTarget target = context.getServiceTarget();
                        target.addService(NamingService.SERVICE_NAME, new NamingService(namingStore))
                            .addAliases(ContextNames.JAVA_CONTEXT_SERVICE_NAME)
                            .setInitialMode(ServiceController.Mode.ACTIVE)
                            .install();

                    // Create the java:global namespace
                    addGlobalContextFactory(target, "global");

                    // Create the EE namespace
                    addContextFactory(target, "app");
                    addContextFactory(target, "module");
                    addContextFactory(target, "comp");

                    // Provide the {@link InitialContext} as OSGi service
                    InitialContextFactoryService.addService(target);

                    final JndiView jndiView = new JndiView();
                    target.addService(ServiceName.JBOSS.append("naming", "jndi", "view"), jndiView)
                            .addDependency(ServiceBuilder.DependencyType.OPTIONAL, ServiceName.JBOSS.append("mbean", "server"), MBeanServer.class, jndiView.getMBeanServerInjector())
                            .install();
                    resultHandler.handleResultComplete();
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }

        final ModelNode compensatingOperation = Util.getResourceRemoveOperation(operation.require(OP_ADDR));

        return new BasicOperationResult(compensatingOperation);
    }

    private static void addContextFactory(final ServiceTarget target, final String contextName) {
        final EEContextService eeContextService = new EEContextService(contextName);
        target.addService(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(contextName), eeContextService)
            .addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME, NamingStore.class, eeContextService.getJavaContextInjector())
            .setInitialMode(ServiceController.Mode.ACTIVE)
            .install();
    }

    private static void addGlobalContextFactory(final ServiceTarget target, final String contextName) {
        final GlobalContextService eeContextService = new GlobalContextService(contextName);
        target.addService(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(contextName), eeContextService)
            .addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME, NamingStore.class, eeContextService.getJavaContextInjector())
            .setInitialMode(ServiceController.Mode.ACTIVE)
            .install();
    }
}

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

package org.jboss.as.capedwarf.extension;

import org.jboss.as.capedwarf.deployment.CapedwarfCDIExtensionProcessor;
import org.jboss.as.capedwarf.deployment.CapedwarfCleanupProcessor;
import org.jboss.as.capedwarf.deployment.CapedwarfDependenciesProcessor;
import org.jboss.as.capedwarf.deployment.CapedwarfDeploymentProcessor;
import org.jboss.as.capedwarf.deployment.CapedwarfInitializationProcessor;
import org.jboss.as.capedwarf.deployment.CapedwarfJPAProcessor;
import org.jboss.as.capedwarf.deployment.CapedwarfPersistenceModificationProcessor;
import org.jboss.as.capedwarf.deployment.CapedwarfWebCleanupProcessor;
import org.jboss.as.capedwarf.deployment.CapedwarfWebComponentsDeploymentProcessor;
import org.jboss.as.capedwarf.deployment.CapedwarfWeldParseProcessor;
import org.jboss.as.capedwarf.deployment.CapedwarfWeldProcessor;
import org.jboss.as.capedwarf.services.ServletExecutorConsumerService;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.Services;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.module.TempFileProviderService;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.vfs.TempDir;
import org.jboss.vfs.VFSUtils;

import javax.jms.Connection;
import java.io.IOException;
import java.util.List;

/**
 * Handler responsible for adding the subsystem resource to the model
 *
 * @author <a href="mailto:marko.luksa@gmail.com">Marko Luksa</a>
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class CapedwarfSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final CapedwarfSubsystemAdd INSTANCE = new CapedwarfSubsystemAdd();
    static final String CAPEDWARF = "capedwarf";

    private CapedwarfSubsystemAdd() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        model.setEmptyObject();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void performBoottime(final OperationContext context, ModelNode operation, ModelNode model,
                                ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {

        final String appengineAPI = operation.hasDefined("appengine-api") ? operation.get("appengine-api").asString() : null;

        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(DeploymentProcessorTarget processorTarget) {
                final ServiceTarget serviceTarget = context.getServiceTarget();

                final ServletExecutorConsumerService consumerService = addQueueConsumer(serviceTarget);

                final TempDir tempDir = createTempDir(serviceTarget);

                final int initialPhaseOrder = Math.min(Phase.PARSE_WEB_DEPLOYMENT, Phase.PARSE_PERSISTENCE_UNIT);
                processorTarget.addDeploymentProcessor(Phase.PARSE, initialPhaseOrder - 20, new CapedwarfInitializationProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, initialPhaseOrder - 10, new CapedwarfPersistenceModificationProcessor(tempDir)); // before persistence.xml parsing
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_WEB_DEPLOYMENT + 1, new CapedwarfWebCleanupProcessor()); // right after web.xml parsing
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_WEB_COMPONENTS - 1, new CapedwarfWebComponentsDeploymentProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_WELD_WEB_INTEGRATION - 10, new CapedwarfWeldParseProcessor()); // before Weld web integration
                processorTarget.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_WELD - 10, new CapedwarfWeldProcessor()); // before Weld
                processorTarget.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_JPA - 10, new CapedwarfJPAProcessor()); // before default JPA processor
                processorTarget.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_WAR_MODULE + 10, new CapedwarfDeploymentProcessor(appengineAPI));
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_APP_NAMING_CONTEXT + 10, new CapedwarfDependenciesProcessor()); // adjust order as needed
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_WELD_PORTABLE_EXTENSIONS + 10, new CapedwarfCDIExtensionProcessor()); // after Weld portable extensions lookup
                processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_MODULE_JNDI_BINDINGS + 1, new CapedwarfCleanupProcessor(consumerService)); // adjust order as needed
            }
        }, OperationContext.Stage.RUNTIME);

    }

    protected static ServletExecutorConsumerService addQueueConsumer(ServiceTarget serviceTarget) {
        final ServletExecutorConsumerService consumerService = new ServletExecutorConsumerService();
        final ServiceBuilder<Connection> builder = serviceTarget.addService(ServletExecutorConsumerService.NAME, consumerService);
        builder.addDependency(ContextNames.bindInfoFor("java:/ConnectionFactory").getBinderServiceName(), ManagedReferenceFactory.class, consumerService.getFactory());
        builder.addDependency(ContextNames.bindInfoFor("java:/queue/" + CAPEDWARF).getBinderServiceName(), ManagedReferenceFactory.class, consumerService.getQueue());
        builder.addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, consumerService.getLoader());
        builder.setInitialMode(ServiceController.Mode.ON_DEMAND).install();
        return consumerService;
    }

    protected static TempDir createTempDir(final ServiceTarget serviceTarget) {
        final TempDir tempDir;
        try {
            tempDir = TempFileProviderService.provider().createTempDir(CAPEDWARF);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot create temp dir for CapeDwarf sub-system!", e);
        }

        final ServiceBuilder<TempDir> builder = serviceTarget.addService(ServiceName.JBOSS.append(CAPEDWARF).append("tempDir"), new Service<TempDir>() {
            public void start(StartContext context) throws StartException {
            }

            public void stop(StopContext context) {
                VFSUtils.safeClose(tempDir);
            }

            public TempDir getValue() throws IllegalStateException, IllegalArgumentException {
                return tempDir;
            }
        });
        builder.setInitialMode(ServiceController.Mode.ACTIVE).install();
        return tempDir;
    }
}

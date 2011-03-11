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

package org.jboss.as.server;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;

import java.io.File;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.server.ServerControllerImpl.RegisteredProcessor;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeployerChainsService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.ServiceLoaderProcessor;
import org.jboss.as.server.deployment.SubDeploymentProcessor;
import org.jboss.as.server.deployment.annotation.AnnotationIndexProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndexProcessor;
import org.jboss.as.server.deployment.api.ServerDeploymentRepository;
import org.jboss.as.server.deployment.module.AdditionalModuleProcessor;
import org.jboss.as.server.deployment.module.DeploymentRootMountProcessor;
import org.jboss.as.server.deployment.module.DeploymentStructureDescriptorParser;
import org.jboss.as.server.deployment.module.ManifestAttachmentProcessor;
import org.jboss.as.server.deployment.module.ManifestClassPathProcessor;
import org.jboss.as.server.deployment.module.ManifestExtensionListProcessor;
import org.jboss.as.server.deployment.module.ManifestExtensionNameProcessor;
import org.jboss.as.server.deployment.module.ModuleClassPathProcessor;
import org.jboss.as.server.deployment.module.ModuleDependencyProcessor;
import org.jboss.as.server.deployment.module.ModuleExtensionListProcessor;
import org.jboss.as.server.deployment.module.ModuleExtensionNameProcessor;
import org.jboss.as.server.deployment.module.ModuleIdentifierProcessor;
import org.jboss.as.server.deployment.module.ModuleSpecProcessor;
import org.jboss.as.server.deployment.module.SubDeploymentDependencyProcessor;
import org.jboss.as.server.deployment.reflect.InstallReflectionIndexProcessor;
import org.jboss.as.server.deployment.service.ServiceActivatorDependencyProcessor;
import org.jboss.as.server.deployment.service.ServiceActivatorProcessor;
import org.jboss.as.server.moduleservice.ExtensionIndexService;
import org.jboss.as.server.moduleservice.ExternalModuleService;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.threads.JBossThreadFactory;

/**
 * The root service of the JBoss Application Server.  Stopping this
 * service will shut down the whole works.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ServerControllerService implements Service<ServerController> {

    private static final Logger log = Logger.getLogger("org.jboss.as.server");

    private static final int DEFAULT_POOL_SIZE = 5;

    private final Bootstrap.Configuration configuration;

    private final InjectedValue<ServerDeploymentRepository> injectedDeploymentRepository = new InjectedValue<ServerDeploymentRepository>();
    private final InjectedValue<ServiceModuleLoader> injectedModuleLoader = new InjectedValue<ServiceModuleLoader>();

    private final InjectedValue<ExternalModuleService> injectedExternalModuleServie = new InjectedValue<ExternalModuleService>();

    // mutable state
    private ServerController serverController;

    public ServerControllerService(final Bootstrap.Configuration configuration) {
        this.configuration = configuration;
    }

    public static void addService(final ServiceTarget serviceTarget, final Bootstrap.Configuration configuration) {
        ServerControllerService service = new ServerControllerService(configuration);
        ServiceBuilder<?> serviceBuilder = serviceTarget.addService(Services.JBOSS_SERVER_CONTROLLER, service);
        serviceBuilder.addDependency(ServerDeploymentRepository.SERVICE_NAME,ServerDeploymentRepository.class, service.injectedDeploymentRepository);
        serviceBuilder.addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ServiceModuleLoader.class, service.injectedModuleLoader);
        serviceBuilder.addDependency(Services.JBOSS_EXTERNAL_MODULE_SERVICE, ExternalModuleService.class,
                service.injectedExternalModuleServie);
        serviceBuilder.install();
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void start(final StartContext context) throws StartException {
        final ServiceContainer container = context.getController().getServiceContainer();
        final ServiceTarget serviceTarget = context.getChildTarget();
        final Bootstrap.Configuration configuration = this.configuration;
        final ServerEnvironment serverEnvironment = configuration.getServerEnvironment();

        final ExtensibleConfigurationPersister persister = configuration.getConfigurationPersister();

        // TODO consider injecting the executor service
        final ThreadGroup threadGroup = new ThreadGroup("ServerController-threads");
        ThreadFactory threadFactory = new JBossThreadFactory(threadGroup, Boolean.FALSE, null, null, null, null, AccessController.getContext());
        final ExecutorService executorService = Executors.newScheduledThreadPool(DEFAULT_POOL_SIZE, threadFactory);
        final ServerControllerImpl serverController = new ServerControllerImpl(container, serviceTarget, serverEnvironment, persister, injectedDeploymentRepository.getValue(), executorService);
        serverController.init();

        final List<ModelNode> updates;
        try {
            updates = persister.load();
        } catch (Exception e) {
            throw new StartException(e);
        }

        log.info("Activating core services");

        final AtomicInteger count = new AtomicInteger(1);
        final ResultHandler resultHandler = new ResultHandler() {
            @Override
            public void handleResultFragment(final String[] location, final ModelNode result) {
            }

            @Override
            public void handleResultComplete() {
                if (count.decrementAndGet() == 0) {
                    // some action
                }
            }

            @Override
            public void handleFailed(final ModelNode failureDescription) {
                log.errorf("Boot update failed: %s", failureDescription);
                if (count.decrementAndGet() == 0) {
                    // some action
                }
            }

            @Override
            public void handleCancellation() {
                if (count.decrementAndGet() == 0) {
                    // some action
                }
            }
        };
        for (ModelNode update : updates) {
            count.incrementAndGet();
            update.get(ROLLBACK_ON_RUNTIME_FAILURE).set(false);
            serverController.execute(OperationBuilder.Factory.create(update).build(), resultHandler);
        }
        if (count.decrementAndGet() == 0) {
            // some action?
        }

        final EnumMap<Phase, SortedSet<RegisteredProcessor>> deployers = serverController.finishBoot();

        final File[] extDirs = serverEnvironment.getJavaExtDirs();
        final File[] newExtDirs = Arrays.copyOf(extDirs, extDirs.length + 1);
        newExtDirs[extDirs.length] = new File(serverEnvironment.getServerBaseDir(), "lib/ext");
        serviceTarget.addService(org.jboss.as.server.deployment.Services.JBOSS_DEPLOYMENT_EXTENSION_INDEX,
                new ExtensionIndexService(newExtDirs)).setInitialMode(ServiceController.Mode.ON_DEMAND).install();

        // Activate  module loader
        deployers.get(Phase.STRUCTURE).add(new RegisteredProcessor(Phase.STRUCTURE_SERVICE_MODULE_LOADER, new DeploymentUnitProcessor() {
            @Override
            public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
                phaseContext.getDeploymentUnit().putAttachment(Attachments.SERVICE_MODULE_LOADER, injectedModuleLoader.getValue());
                phaseContext.getDeploymentUnit().putAttachment(Attachments.EXTERNAL_MODULE_SERVICE, injectedExternalModuleServie.getValue());
            }

            @Override
            public void undeploy(DeploymentUnit context) {
                context.removeAttachment(Attachments.SERVICE_MODULE_LOADER);
            }
        }));

        // Activate core processors for jar deployment
        deployers.get(Phase.STRUCTURE).add(new RegisteredProcessor(Phase.STRUCTURE_MOUNT, new DeploymentRootMountProcessor()));
        deployers.get(Phase.STRUCTURE).add(new RegisteredProcessor(Phase.STRUCTURE_MANIFEST, new ManifestAttachmentProcessor()));
        deployers.get(Phase.STRUCTURE).add(new RegisteredProcessor(Phase.STRUCTURE_SUB_DEPLOYMENT, new SubDeploymentProcessor()));
        deployers.get(Phase.STRUCTURE).add(new RegisteredProcessor(Phase.STRUCTURE_MODULE_IDENTIFIERS, new ModuleIdentifierProcessor()));
        deployers.get(Phase.STRUCTURE).add(new RegisteredProcessor(Phase.STRUCTURE_ANNOTATION_INDEX, new AnnotationIndexProcessor()));
        deployers.get(Phase.PARSE).add(new RegisteredProcessor(Phase.PARSE_STRUCTURE_DESCRIPTOR, new DeploymentStructureDescriptorParser()));
        deployers.get(Phase.PARSE).add(new RegisteredProcessor(Phase.PARSE_COMPOSITE_ANNOTATION_INDEX, new CompositeIndexProcessor()));
        deployers.get(Phase.PARSE).add(new RegisteredProcessor(Phase.PARSE_ADDITIONAL_MODULES, new AdditionalModuleProcessor()));
        deployers.get(Phase.PARSE).add(new RegisteredProcessor(Phase.PARSE_CLASS_PATH, new ManifestClassPathProcessor()));
        deployers.get(Phase.PARSE).add(new RegisteredProcessor(Phase.PARSE_EXTENSION_LIST, new ManifestExtensionListProcessor()));
        deployers.get(Phase.PARSE).add(new RegisteredProcessor(Phase.PARSE_EXTENSION_NAME, new ManifestExtensionNameProcessor()));
        deployers.get(Phase.PARSE).add(new RegisteredProcessor(Phase.PARSE_SERVICE_LOADER_DEPLOYMENT, new ServiceLoaderProcessor()));
        deployers.get(Phase.DEPENDENCIES).add(new RegisteredProcessor(Phase.DEPENDENCIES_MODULE, new ModuleDependencyProcessor()));
        deployers.get(Phase.DEPENDENCIES).add(new RegisteredProcessor(Phase.DEPENDENCIES_SAR_MODULE, new ServiceActivatorDependencyProcessor()));
        deployers.get(Phase.DEPENDENCIES).add(new RegisteredProcessor(Phase.DEPENDENCIES_CLASS_PATH, new ModuleClassPathProcessor()));
        deployers.get(Phase.DEPENDENCIES).add(new RegisteredProcessor(Phase.DEPENDENCIES_EXTENSION_LIST, new ModuleExtensionListProcessor()));
        deployers.get(Phase.DEPENDENCIES).add(new RegisteredProcessor(Phase.DEPENDENCIES_SUB_DEPLOYMENTS, new SubDeploymentDependencyProcessor()));
        deployers.get(Phase.CONFIGURE_MODULE).add(new RegisteredProcessor(Phase.CONFIGURE_MODULE_SPEC, new ModuleSpecProcessor()));
        deployers.get(Phase.POST_MODULE).add(new RegisteredProcessor(Phase.POST_MODULE_INSTALL_EXTENSION, new ModuleExtensionNameProcessor()));
        deployers.get(Phase.INSTALL).add(new RegisteredProcessor(Phase.INSTALL_REFLECTION_INDEX, new InstallReflectionIndexProcessor()));
        deployers.get(Phase.INSTALL).add(new RegisteredProcessor(Phase.INSTALL_SERVICE_ACTIVATOR, new ServiceActivatorProcessor()));

        // All deployers are registered

        final EnumMap<Phase, List<DeploymentUnitProcessor>> finalDeployers = new EnumMap<Phase, List<DeploymentUnitProcessor>>(Phase.class);
        for (Map.Entry<Phase, SortedSet<RegisteredProcessor>> entry : deployers.entrySet()) {
            final SortedSet<RegisteredProcessor> processorSet = entry.getValue();
            final List<DeploymentUnitProcessor> list = new ArrayList<DeploymentUnitProcessor>(processorSet.size());
            for (RegisteredProcessor processor : processorSet) {
                list.add(processor.getProcessor());
            }
            finalDeployers.put(entry.getKey(), list);
        }

        DeployerChainsService.addService(serviceTarget, finalDeployers);

        this.serverController = serverController;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void stop(final StopContext context) {
        serverController = null;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized ServerController getValue() throws IllegalStateException, IllegalArgumentException {
        return serverController;
    }
}

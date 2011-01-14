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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.persistence.NewConfigurationPersister;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeployerChainsService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.SubDeploymentProcessor;
import org.jboss.as.server.deployment.annotation.AnnotationIndexProcessor;
import org.jboss.as.server.deployment.api.ServerDeploymentRepository;
import org.jboss.as.server.deployment.impl.ServerDeploymentRepositoryImpl;
import org.jboss.as.server.deployment.module.DeploymentModuleLoader;
import org.jboss.as.server.deployment.module.DeploymentModuleLoaderImpl;
import org.jboss.as.server.deployment.module.DeploymentRootMountProcessor;
import org.jboss.as.server.deployment.module.ManifestAttachmentProcessor;
import org.jboss.as.server.deployment.module.ManifestClassPathProcessor;
import org.jboss.as.server.deployment.module.ManifestExtensionListProcessor;
import org.jboss.as.server.deployment.module.ModuleDependencyProcessor;
import org.jboss.as.server.deployment.module.ModuleDeploymentProcessor;
import org.jboss.as.server.deployment.module.ModuleSpecProcessor;
import org.jboss.as.server.deployment.service.ServiceActivatorDependencyProcessor;
import org.jboss.as.server.deployment.service.ServiceActivatorProcessor;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.LifecycleContext;
import org.jboss.msc.service.MultipleRemoveListener;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.service.TrackingServiceTarget;

/**
 * The root service of the JBoss Application Server.  Stopping this
 * service will shut down the whole works.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class NewServerControllerService implements Service<NewServerController> {

    private static final Logger log = Logger.getLogger("org.jboss.as.server");

    private final NewBootstrap.Configuration configuration;

    private DeploymentModuleLoader deploymentModuleLoader;

    // mutable state
    private NewServerController serverController;
    private Set<ServiceName> bootServices;

    public NewServerControllerService(final NewBootstrap.Configuration configuration) {
        this.configuration = configuration;
    }

    /** {@inheritDoc} */
    public synchronized void start(final StartContext context) throws StartException {
        final ServiceContainer container = context.getController().getServiceContainer();
        final TrackingServiceTarget serviceTarget = new TrackingServiceTarget(container.subTarget());
        serviceTarget.addDependency(context.getController().getName());
        final NewBootstrap.Configuration configuration = this.configuration;
        final ServerEnvironment serverEnvironment = configuration.getServerEnvironment();

        final int threads = (int) (Runtime.getRuntime().availableProcessors() * 1.5f);
        final ExecutorService executor = new ThreadPoolExecutor(threads, threads, Long.MAX_VALUE, TimeUnit.NANOSECONDS, new LinkedBlockingQueue<Runnable>());
        container.setExecutor(executor);

        final NewConfigurationPersister persister = configuration.getConfigurationPersister();

        final NewServerControllerImpl serverController = new NewServerControllerImpl(container, serverEnvironment, persister);
        serverController.init();

        final List<ModelNode> updates;
        try {
            updates = persister.load();
        } catch (Exception e) {
            throw new StartException(e);
        }

        final EnumMap<Phase, SortedSet<RegisteredProcessor>> deployers = new EnumMap<Phase, SortedSet<RegisteredProcessor>>(Phase.class);
        for (Phase phase : Phase.values()) {
            deployers.put(phase, new ConcurrentSkipListSet<RegisteredProcessor>());
        }

        log.info("Activating core services");

        final AtomicInteger count = new AtomicInteger(1);
        final ResultHandler resultHandler = new ResultHandler() {
            public void handleResultFragment(final String[] location, final ModelNode result) {
            }

            public void handleResultComplete(final ModelNode compensatingOperation) {
                if (count.decrementAndGet() == 0) {
                    // some action
                }
            }

            public void handleFailed(final ModelNode failureDescription) {
                if (count.decrementAndGet() == 0) {
                    // some action
                }
            }

            public void handleCancellation() {
                if (count.decrementAndGet() == 0) {
                    // some action
                }
            }
        };
        for (ModelNode update : updates) {
            count.incrementAndGet();
            serverController.execute(update, resultHandler);
        }
        if (count.decrementAndGet() == 0) {
            // some action?
        }
        serverController.finishBoot();

        serviceTarget.addService(ServerDeploymentRepository.SERVICE_NAME,
            new ServerDeploymentRepositoryImpl(serverEnvironment.getServerDeployDir(), serverEnvironment.getServerSystemDeployDir()))
            .install();

        // Activate deployment module loader
        deploymentModuleLoader = new DeploymentModuleLoaderImpl(configuration.getModuleLoader());
        deployers.get(Phase.STRUCTURE).add(new RegisteredProcessor(Phase.STRUCTURE_DEPLOYMENT_MODULE_LOADER, new DeploymentUnitProcessor() {
            public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
                phaseContext.getDeploymentUnit().putAttachment(Attachments.DEPLOYMENT_MODULE_LOADER, deploymentModuleLoader);
            }

            public void undeploy(DeploymentUnit context) {
                context.removeAttachment(Attachments.DEPLOYMENT_MODULE_LOADER);
            }
        }));

        // Activate core processors for jar deployment
        deployers.get(Phase.STRUCTURE).add(new RegisteredProcessor(Phase.STRUCTURE_MOUNT, new DeploymentRootMountProcessor()));
        deployers.get(Phase.STRUCTURE).add(new RegisteredProcessor(Phase.STRUCTURE_SUB_DEPLOYMENT, new SubDeploymentProcessor()));
        deployers.get(Phase.PARSE).add(new RegisteredProcessor(Phase.PARSE_MANIFEST, new ManifestAttachmentProcessor()));
        deployers.get(Phase.PARSE).add(new RegisteredProcessor(Phase.PARSE_CLASS_PATH, new ManifestClassPathProcessor()));
        deployers.get(Phase.PARSE).add(new RegisteredProcessor(Phase.PARSE_EXTENSION_LIST, new ManifestExtensionListProcessor()));
        deployers.get(Phase.PARSE).add(new RegisteredProcessor(Phase.STRUCTURE_ANNOTATION_INDEX, new AnnotationIndexProcessor()));
        deployers.get(Phase.DEPENDENCIES).add(new RegisteredProcessor(Phase.DEPENDENCIES_MODULE, new ModuleDependencyProcessor()));
        deployers.get(Phase.DEPENDENCIES).add(new RegisteredProcessor(Phase.DEPENDENCIES_SAR_MODULE, new ServiceActivatorDependencyProcessor()));
        deployers.get(Phase.CONFIGURE_MODULE).add(new RegisteredProcessor(Phase.CONFIGURE_MODULE_SPEC, new ModuleSpecProcessor()));
        deployers.get(Phase.MODULARIZE).add(new RegisteredProcessor(Phase.MODULARIZE_DEPLOYMENT, new ModuleDeploymentProcessor()));
        deployers.get(Phase.DEPENDENCIES).add(new RegisteredProcessor(Phase.DEPENDENCIES_SAR_MODULE, new ServiceActivatorDependencyProcessor()));
        deployers.get(Phase.INSTALL).add(new RegisteredProcessor(Phase.INSTALL_SERVICE_ACTIVATOR, new ServiceActivatorProcessor()));

        // All deployers are registered

        final EnumMap<Phase, List<DeploymentUnitProcessor>> finalDeployers = new EnumMap<Phase, List<DeploymentUnitProcessor>>(Phase.class);
        for (Map.Entry<Phase, SortedSet<RegisteredProcessor>> entry : deployers.entrySet()) {
            final SortedSet<RegisteredProcessor> processorSet = entry.getValue();
            final List<DeploymentUnitProcessor> list = new ArrayList<DeploymentUnitProcessor>(processorSet.size());
            for (RegisteredProcessor processor : processorSet) {
                list.add(processor.processor);
            }
            finalDeployers.put(entry.getKey(), list);
        }

        DeployerChainsService.addService(serviceTarget, finalDeployers);

        this.serverController = serverController;
        bootServices = serviceTarget.getSet();
    }

    /** {@inheritDoc} */
    public synchronized void stop(final StopContext context) {
        serverController = null;
        final ServiceContainer container = context.getController().getServiceContainer();
        final Set<ServiceName> bootServices = this.bootServices;
        context.asynchronous();
        MultipleRemoveListener<LifecycleContext> listener = MultipleRemoveListener.create(context);
        for (ServiceName serviceName : bootServices) {
            final ServiceController<?> controller = container.getService(serviceName);
            if (controller != null) {
                controller.addListener(listener);
                controller.setMode(ServiceController.Mode.REMOVE);
            }
        }
        // tick the count down
        listener.done();
    }

    /** {@inheritDoc} */
    public synchronized NewServerController getValue() throws IllegalStateException, IllegalArgumentException {
        return serverController;
    }

    private static final class RegisteredProcessor implements Comparable<RegisteredProcessor> {
        private final int priority;
        private final DeploymentUnitProcessor processor;

        private RegisteredProcessor(final int priority, final DeploymentUnitProcessor processor) {
            this.priority = priority;
            this.processor = processor;
        }

        public int compareTo(final RegisteredProcessor o) {
            final int rel = Integer.signum(priority - o.priority);
            return rel == 0 ? processor.getClass().getName().compareTo(o.getClass().getName()) : rel;
        }
    }
}

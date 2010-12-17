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

package org.jboss.as.server;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import org.jboss.as.naming.deployment.ModuleContextProcessor;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeployerChainsService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.BootUpdateContext;
import org.jboss.as.model.ServerModel;
import org.jboss.as.server.deployment.api.ServerDeploymentRepository;
import org.jboss.as.server.deployment.impl.ServerDeploymentRepositoryImpl;
import org.jboss.as.server.deployment.annotation.AnnotationIndexProcessor;
import org.jboss.as.server.deployment.module.DeploymentModuleLoader;
import org.jboss.as.server.deployment.module.DeploymentModuleLoaderImpl;
import org.jboss.as.server.deployment.module.ManifestAttachmentProcessor;
import org.jboss.as.server.deployment.module.ModuleDependencyProcessor;
import org.jboss.as.server.deployment.module.ModuleDeploymentProcessor;
import org.jboss.as.server.deployment.service.ServiceActivatorDependencyProcessor;
import org.jboss.as.server.deployment.service.ServiceActivatorProcessor;
import org.jboss.as.server.mgmt.ServerConfigurationPersister;
import org.jboss.as.server.mgmt.ShutdownHandlerImpl;
import org.jboss.as.server.services.net.SocketBindingManager;
import org.jboss.as.server.services.net.SocketBindingManagerService;
import org.jboss.as.version.Version;
import org.jboss.logging.Logger;
import org.jboss.msc.service.DelegatingServiceRegistry;
import org.jboss.msc.service.MultipleRemoveListener;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
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
final class ServerControllerService implements Service<ServerController> {

    public static final ServiceName JBOSS_AS_NAME = ServiceName.JBOSS.append("as");

    private static final Logger log = Logger.getLogger("org.jboss.as.server");

    private final Bootstrap.Configuration configuration;

    private final List<ServiceActivator> extraServices;

    private DeploymentModuleLoader deploymentModuleLoader;

    // mutable state
    private ServerController serverController;
    private Set<ServiceName> bootServices;

    public ServerControllerService(final Bootstrap.Configuration configuration, final List<ServiceActivator> extraServices) {
        this.configuration = configuration;
        this.extraServices = extraServices;
    }

    /** {@inheritDoc} */
    public synchronized void start(final StartContext context) throws StartException {
        final ServiceContainer container = context.getController().getServiceContainer();
        final TrackingServiceTarget serviceTarget = new TrackingServiceTarget(container);
        serviceTarget.addDependency(context.getController().getName());
        final DelegatingServiceRegistry serviceRegistry = new DelegatingServiceRegistry(container);
        final Bootstrap.Configuration configuration = this.configuration;
        final ServerEnvironment serverEnvironment = configuration.getServerEnvironment();

        // Install the environment before fetching and using the persister
        serverEnvironment.install();

        final ServiceActivatorContext serviceActivatorContext = new ServiceActivatorContext() {
            public ServiceTarget getServiceTarget() {
                return serviceTarget;
            }

            public ServiceRegistry getServiceRegistry() {
                return container;
            }
        };

        for(ServiceActivator activator : extraServices) {
            activator.activate(serviceActivatorContext);
        }

        final ServerModel serverModel = new ServerModel(serverEnvironment.getServerName(), configuration.getPortOffset());

        final ServerControllerImpl serverController = new ServerControllerImpl(serverModel, container, serverEnvironment);

        final ServerConfigurationPersister persister = configuration.getConfigurationPersister();
        final List<AbstractServerModelUpdate<?>> updates;
        try {
            updates = persister.load(serverController);
        } catch (Exception e) {
            throw new StartException(e);
        }

        final EnumMap<Phase, SortedSet<RegisteredProcessor>> deployers = new EnumMap<Phase, SortedSet<RegisteredProcessor>>(Phase.class);
        for (Phase phase : Phase.values()) {
            deployers.put(phase, new ConcurrentSkipListSet<RegisteredProcessor>());
        }
        final BootUpdateContext bootUpdateContext = new BootUpdateContext() {

            public ServiceTarget getServiceTarget() {
                return serviceTarget;
            }

            public ServiceRegistry getServiceRegistry() {
                return serviceRegistry;
            }

            public void addDeploymentProcessor(final Phase phase, final int priority, final DeploymentUnitProcessor processor) {
                if (phase == null) {
                    throw new IllegalArgumentException("phase is null");
                }
                if (processor == null) {
                    throw new IllegalArgumentException("processor is null");
                }
                if (priority < 0) {
                    throw new IllegalArgumentException("priority is invalid (must be >= 0)");
                }
                deployers.get(phase).add(new RegisteredProcessor(priority, processor));
            }

            public ServerEnvironment getServerEnvironment() {
                return serverEnvironment;
            }
        };

        log.info("Activating core services");

        // TODO: decide the fate of these

        // Graceful shutdown
        ShutdownHandlerImpl.addService(serviceTarget);

        // Server environment services; todo: drop environment service, fold in path services
        ServerEnvironmentServices.addServices(serverEnvironment, serviceTarget);

        serviceTarget.addService(ServerModel.SERVICE_NAME, new Service<ServerModel>() {
            public void start(StartContext context) throws StartException {
            }

            public void stop(StopContext context) {
            }

            public ServerModel getValue() throws IllegalStateException, IllegalArgumentException {
                return serverModel;
            }
        }).install();

        // Socket binding manager
        serviceTarget.addService(SocketBindingManager.SOCKET_BINDING_MANAGER,
            new SocketBindingManagerService(configuration.getPortOffset()))
            .setInitialMode(ServiceController.Mode.ON_DEMAND)
            .install();

        for (AbstractServerModelUpdate<?> update : updates) {
            update.applyUpdateBootAction(bootUpdateContext);
        }

        serviceTarget.addService(ServerDeploymentRepository.SERVICE_NAME,
            new ServerDeploymentRepositoryImpl(serverEnvironment.getServerDeployDir(), serverEnvironment.getServerSystemDeployDir()))
            .install();

        // Activate deployment module loader
        deploymentModuleLoader = new DeploymentModuleLoaderImpl(configuration.getModuleLoader());
        deployers.get(Phase.MODULARIZE).add(new RegisteredProcessor(Phase.MODULARIZE_DEPLOYMENT_MODULE_LOADER, new DeploymentUnitProcessor() {
            public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
                phaseContext.getDeploymentUnit().putAttachment(Attachments.DEPLOYMENT_MODULE_LOADER, deploymentModuleLoader);
            }

            public void undeploy(DeploymentUnit context) {
                context.removeAttachment(Attachments.DEPLOYMENT_MODULE_LOADER);
            }
        }));

        // Activate core processors for jar deployment
        deployers.get(Phase.PARSE).add(new RegisteredProcessor(Phase.PARSE_MANIFEST, new ManifestAttachmentProcessor()));
        deployers.get(Phase.PARSE).add(new RegisteredProcessor(Phase.PARSE_ANNOTATION_INDEX, new AnnotationIndexProcessor()));
        deployers.get(Phase.DEPENDENCIES).add(new RegisteredProcessor(Phase.DEPENDENCIES_MODULE, new ModuleDependencyProcessor()));
        deployers.get(Phase.MODULARIZE).add(new RegisteredProcessor(Phase.MODULARIZE_DEPLOYMENT, new ModuleDeploymentProcessor()));
        deployers.get(Phase.INSTALL).add(new RegisteredProcessor(Phase.INSTALL_MODULE_CONTEXT, new ModuleContextProcessor()));
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
        Logger.getLogger("org.jboss.as").infof("Shutdown requested; stopping all services");
        serverController = null;
        final ServiceContainer container = context.getController().getServiceContainer();
        final Set<ServiceName> bootServices = this.bootServices;
        context.asynchronous();
        final MultipleRemoveListener<Runnable> removeListener = MultipleRemoveListener.create(new Runnable() {
            public void run() {
                context.complete();
                Logger.getLogger("org.jboss.as").infof("JBoss AS %s \"%s\" stopped in %dms", Version.AS_VERSION, Version.AS_RELEASE_CODENAME, Integer.valueOf((int) (context.getElapsedTime() / 1000000L)));
            }
        });
        for (ServiceName serviceName : bootServices) {
            final ServiceController<?> controller = container.getService(serviceName);
            if (controller != null) {
                controller.addListener(removeListener);
                controller.setMode(ServiceController.Mode.REMOVE);
            }
        }
        // tick the count down
        removeListener.done();
    }

    /** {@inheritDoc} */
    public synchronized ServerController getValue() throws IllegalStateException, IllegalArgumentException {
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

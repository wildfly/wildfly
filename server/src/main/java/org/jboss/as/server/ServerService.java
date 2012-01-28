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

import java.io.File;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.AbstractControllerService;
import org.jboss.as.controller.BootContext;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.platform.mbean.PlatformMBeanConstants;
import org.jboss.as.platform.mbean.RootPlatformMBeanResource;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.server.controller.descriptions.ServerDescriptionProviders;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentMountProvider;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.ServiceLoaderProcessor;
import org.jboss.as.server.deployment.SubDeploymentProcessor;
import org.jboss.as.server.deployment.annotation.AnnotationIndexProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndexProcessor;
import org.jboss.as.server.deployment.integration.Seam2Processor;
import org.jboss.as.server.deployment.module.ClassFileTransformerProcessor;
import org.jboss.as.server.deployment.module.DeploymentRootExplodedMountProcessor;
import org.jboss.as.server.deployment.module.DeploymentRootMountProcessor;
import org.jboss.as.server.deployment.module.DeploymentVisibilityProcessor;
import org.jboss.as.server.deployment.module.DriverDependenciesProcessor;
import org.jboss.as.server.deployment.module.ManifestAttachmentProcessor;
import org.jboss.as.server.deployment.module.ManifestClassPathProcessor;
import org.jboss.as.server.deployment.module.ManifestDependencyProcessor;
import org.jboss.as.server.deployment.module.ManifestExtensionListProcessor;
import org.jboss.as.server.deployment.module.ManifestExtensionNameProcessor;
import org.jboss.as.server.deployment.module.ModuleClassPathProcessor;
import org.jboss.as.server.deployment.module.ModuleDependencyProcessor;
import org.jboss.as.server.deployment.module.ModuleExtensionListProcessor;
import org.jboss.as.server.deployment.module.ModuleExtensionNameProcessor;
import org.jboss.as.server.deployment.module.ModuleIdentifierProcessor;
import org.jboss.as.server.deployment.module.ModuleSpecProcessor;
import org.jboss.as.server.deployment.module.ServerDependenciesProcessor;
import org.jboss.as.server.deployment.module.SubDeploymentDependencyProcessor;
import org.jboss.as.server.deployment.module.descriptor.DeploymentStructureDescriptorParser;
import org.jboss.as.server.deployment.reflect.InstallReflectionIndexProcessor;
import org.jboss.as.server.deployment.service.ServiceActivatorDependencyProcessor;
import org.jboss.as.server.deployment.service.ServiceActivatorProcessor;
import org.jboss.as.server.mgmt.domain.RemoteFileRepository;
import org.jboss.as.server.moduleservice.ExtensionIndexService;
import org.jboss.as.server.moduleservice.ExternalModuleService;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.as.server.services.security.AbstractVaultReader;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.threads.JBossThreadFactory;

/**
 * Service for the {@link ModelController} for an AS server instance.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServerService extends AbstractControllerService {

    private final InjectedValue<DeploymentMountProvider> injectedDeploymentRepository = new InjectedValue<DeploymentMountProvider>();
    private final InjectedValue<ContentRepository> injectedContentRepository = new InjectedValue<ContentRepository>();
    private final InjectedValue<ServiceModuleLoader> injectedModuleLoader = new InjectedValue<ServiceModuleLoader>();

    private final InjectedValue<ExternalModuleService> injectedExternalModuleService = new InjectedValue<ExternalModuleService>();
    private final Bootstrap.Configuration configuration;
    private final BootstrapListener bootstrapListener;
    private final ControlledProcessState processState;
    private final RunningModeControl runningModeControl;
    private volatile ExecutorService queuelessExecutor;
    private volatile ExtensibleConfigurationPersister extensibleConfigurationPersister;
    private final AbstractVaultReader vaultReader;
    private final RemoteFileRepository remoteFileRepository;

    /**
     * Construct a new instance.
     *
     * @param configuration the bootstrap configuration
     * @param prepareStep the prepare step to use
     */
    ServerService(final Bootstrap.Configuration configuration, final ControlledProcessState processState,
                  final OperationStepHandler prepareStep, final BootstrapListener bootstrapListener,
                  final RunningModeControl runningModeControl, final AbstractVaultReader vaultReader, final RemoteFileRepository remoteFileRepository) {
        super(getProcessType(configuration.getServerEnvironment()), runningModeControl, null, processState,
                ServerDescriptionProviders.ROOT_PROVIDER, prepareStep, new RuntimeExpressionResolver(vaultReader));
        this.configuration = configuration;
        this.bootstrapListener = bootstrapListener;
        this.processState = processState;
        this.runningModeControl = runningModeControl;
        this.vaultReader = vaultReader;
        this.remoteFileRepository = remoteFileRepository;
    }

    static ProcessType getProcessType(ServerEnvironment serverEnvironment) {
        if (serverEnvironment != null) {
            switch (serverEnvironment.getLaunchType()) {
            case DOMAIN:
                return ProcessType.DOMAIN_SERVER;
            case STANDALONE:
                return ProcessType.STANDALONE_SERVER;
            case EMBEDDED:
                return ProcessType.EMBEDDED_SERVER;
            case APPCLIENT:
                return ProcessType.APPLICATION_CLIENT;
            }
        }

        return ProcessType.EMBEDDED_SERVER;
    }

    /**
     * Add this service to the given service target.
     *
     * @param serviceTarget the service target
     * @param configuration the bootstrap configuration
     */
    public static void addService(final ServiceTarget serviceTarget, final Bootstrap.Configuration configuration,
                                  final ControlledProcessState processState, final BootstrapListener bootstrapListener,
                                  final RunningModeControl runningModeControl, final AbstractVaultReader vaultReader, final RemoteFileRepository remoteFileRepository) {
        ServerService service = new ServerService(configuration, processState, null, bootstrapListener, runningModeControl, vaultReader, remoteFileRepository);
        ServiceBuilder<?> serviceBuilder = serviceTarget.addService(Services.JBOSS_SERVER_CONTROLLER, service);
        serviceBuilder.addDependency(DeploymentMountProvider.SERVICE_NAME,DeploymentMountProvider.class, service.injectedDeploymentRepository);
        serviceBuilder.addDependency(ContentRepository.SERVICE_NAME, ContentRepository.class, service.injectedContentRepository);
        serviceBuilder.addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ServiceModuleLoader.class, service.injectedModuleLoader);
        serviceBuilder.addDependency(Services.JBOSS_EXTERNAL_MODULE_SERVICE, ExternalModuleService.class,
                service.injectedExternalModuleService);
        serviceBuilder.install();
    }

    public synchronized void start(final StartContext context) throws StartException {
        ServerEnvironment serverEnvironment = configuration.getServerEnvironment();
        initializeExecutorService(serverEnvironment);

        Bootstrap.ConfigurationPersisterFactory configurationPersisterFactory = configuration.getConfigurationPersisterFactory();
        extensibleConfigurationPersister = configurationPersisterFactory.createConfigurationPersister(serverEnvironment, getExecutorServiceInjector().getOptionalValue());
        setConfigurationPersister(extensibleConfigurationPersister);
        super.start(context);
    }

    private void initializeExecutorService(ServerEnvironment serverEnvironment) {
        if (serverEnvironment.isAllowModelControllerExecutor()) {
            final ThreadGroup threadGroup = new ThreadGroup("ModelController ThreadGroup");
            final String namePattern = "ServerService Thread Pool -- %t";
            final ThreadFactory threadFactory = new JBossThreadFactory(threadGroup, Boolean.FALSE, null, namePattern, null, null, AccessController.getContext());
            queuelessExecutor = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                      5L, TimeUnit.SECONDS,
                                      new SynchronousQueue<Runnable>(),
                                      threadFactory);
            getExecutorServiceInjector().inject(queuelessExecutor);
        }
    }

    protected void boot(final BootContext context) throws ConfigurationPersistenceException {
        final ServerEnvironment serverEnvironment = configuration.getServerEnvironment();
        final ServiceTarget serviceTarget = context.getServiceTarget();
        serviceTarget.addListener(ServiceListener.Inheritance.ALL, bootstrapListener);
        final File[] extDirs = serverEnvironment.getJavaExtDirs();
        final File[] newExtDirs = Arrays.copyOf(extDirs, extDirs.length + 1);
        newExtDirs[extDirs.length] = new File(serverEnvironment.getServerBaseDir(), "lib/ext");
        serviceTarget.addService(org.jboss.as.server.deployment.Services.JBOSS_DEPLOYMENT_EXTENSION_INDEX,
                new ExtensionIndexService(newExtDirs)).setInitialMode(ServiceController.Mode.ON_DEMAND).install();


        // Activate module loader
        DeployerChainAddHandler.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_SERVICE_MODULE_LOADER, new DeploymentUnitProcessor() {
            @Override
            public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
                phaseContext.getDeploymentUnit().putAttachment(Attachments.SERVICE_MODULE_LOADER, injectedModuleLoader.getValue());
                phaseContext.getDeploymentUnit().putAttachment(Attachments.EXTERNAL_MODULE_SERVICE, injectedExternalModuleService.getValue());
            }

            @Override
            public void undeploy(DeploymentUnit context) {
                context.removeAttachment(Attachments.SERVICE_MODULE_LOADER);
            }
        });

        // Activate core processors for jar deployment
        DeployerChainAddHandler.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_EXPLODED_MOUNT, new DeploymentRootExplodedMountProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_MOUNT, new DeploymentRootMountProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_MANIFEST, new ManifestAttachmentProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_ADDITIONAL_MANIFEST, new ManifestAttachmentProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_SUB_DEPLOYMENT, new SubDeploymentProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_MODULE_IDENTIFIERS, new ModuleIdentifierProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_ANNOTATION_INDEX, new AnnotationIndexProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_JBOSS_DEPLOYMENT_STRUCTURE_DESCRIPTOR, new DeploymentStructureDescriptorParser());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_CLASS_PATH, new ManifestClassPathProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_DEPENDENCIES_MANIFEST, new ManifestDependencyProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_COMPOSITE_ANNOTATION_INDEX, new CompositeIndexProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EXTENSION_LIST, new ManifestExtensionListProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EXTENSION_NAME, new ManifestExtensionNameProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_SERVICE_LOADER_DEPLOYMENT, new ServiceLoaderProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_MODULE, new ModuleDependencyProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_SAR_MODULE, new ServiceActivatorDependencyProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_CLASS_PATH, new ModuleClassPathProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_EXTENSION_LIST, new ModuleExtensionListProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_SUB_DEPLOYMENTS, new SubDeploymentDependencyProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_JDK, new ServerDependenciesProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_VISIBLE_MODULES, new DeploymentVisibilityProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_DRIVERS, new DriverDependenciesProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.CONFIGURE_MODULE, Phase.CONFIGURE_MODULE_SPEC, new ModuleSpecProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_INSTALL_EXTENSION, new ModuleExtensionNameProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_REFLECTION_INDEX, new InstallReflectionIndexProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_TRANSFORMER, new ClassFileTransformerProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_SERVICE_ACTIVATOR, new ServiceActivatorProcessor());

        // Ext integration deployers

        DeployerChainAddHandler.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_SEAM, new Seam2Processor(serviceTarget));

        try {
            super.boot(context);
        } finally {
            DeployerChainAddHandler.INSTANCE.clearDeployerMap();
        }

        bootstrapListener.tick();
    }

    protected void boot(List<ModelNode> bootOperations) throws ConfigurationPersistenceException {
        final List<ModelNode> operations = new ArrayList<ModelNode>(bootOperations);
        operations.add(DeployerChainAddHandler.OPERATION);
        super.boot(operations);
    }

    public void stop(final StopContext context) {
        super.stop(context);

        configuration.getExtensionRegistry().clear();
        configuration.getServerEnvironment().resetProvidedProperties();

        if (queuelessExecutor != null) {
            context.asynchronous();
            Thread executorShutdown = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        queuelessExecutor.shutdown();
                    } finally {
                        queuelessExecutor = null;
                        context.complete();
                    }
                }
            }, "Controller ExecutorService Shutdown Thread");
            executorShutdown.start();
        }
    }

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
        ServerControllerModelUtil.updateCoreModel(rootResource.getModel(), configuration.getServerEnvironment());
        ServerControllerModelUtil.initOperations(rootRegistration, injectedContentRepository.getValue(),
                extensibleConfigurationPersister, configuration.getServerEnvironment(), processState,
                runningModeControl, vaultReader, configuration.getExtensionRegistry(), queuelessExecutor != null, remoteFileRepository);

        // TODO maybe make creating of empty nodes part of the MNR description
        rootResource.registerChild(PathElement.pathElement(ModelDescriptionConstants.CORE_SERVICE, ModelDescriptionConstants.MANAGEMENT), Resource.Factory.create());
        rootResource.registerChild(PathElement.pathElement(ModelDescriptionConstants.CORE_SERVICE, ModelDescriptionConstants.SERVICE_CONTAINER), Resource.Factory.create());
        rootResource.registerChild(ServerEnvironmentResourceDescription.RESOURCE_PATH, Resource.Factory.create());

        // Platform MBeans
        rootResource.registerChild(PlatformMBeanConstants.ROOT_PATH, new RootPlatformMBeanResource());
    }
}

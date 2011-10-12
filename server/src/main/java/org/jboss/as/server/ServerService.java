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

import org.jboss.as.controller.AbstractControllerService;
import org.jboss.as.controller.BootContext;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.platform.mbean.PlatformMBeanConstants;
import org.jboss.as.platform.mbean.RootPlatformMBeanResource;
import org.jboss.as.server.controller.descriptions.ServerDescriptionProviders;
import org.jboss.as.server.deployment.Attachments;
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
import org.jboss.as.server.deployment.module.AdditionalModuleProcessor;
import org.jboss.as.server.deployment.module.ClassFileTransformerProcessor;
import org.jboss.as.server.deployment.module.DeploymentRootExplodedMountProcessor;
import org.jboss.as.server.deployment.module.DeploymentRootMountProcessor;
import org.jboss.as.server.deployment.module.DeploymentStructureDescriptorParser;
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
import org.jboss.as.server.deployment.module.ModuleInformationServiceProcessor;
import org.jboss.as.server.deployment.module.ModuleSpecProcessor;
import org.jboss.as.server.deployment.module.ServerDependenciesProcessor;
import org.jboss.as.server.deployment.module.SubDeploymentDependencyProcessor;
import org.jboss.as.server.deployment.reflect.InstallReflectionIndexProcessor;
import org.jboss.as.server.deployment.repository.api.ContentRepository;
import org.jboss.as.server.deployment.repository.api.ServerDeploymentRepository;
import org.jboss.as.server.deployment.service.ServiceActivatorDependencyProcessor;
import org.jboss.as.server.deployment.service.ServiceActivatorProcessor;
import org.jboss.as.server.moduleservice.ExtensionIndexService;
import org.jboss.as.server.moduleservice.ExternalModuleService;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServerService extends AbstractControllerService {

    private final InjectedValue<ServerDeploymentRepository> injectedDeploymentRepository = new InjectedValue<ServerDeploymentRepository>();
    private final InjectedValue<ContentRepository> injectedContentRepository = new InjectedValue<ContentRepository>();
    private final InjectedValue<ServiceModuleLoader> injectedModuleLoader = new InjectedValue<ServiceModuleLoader>();

    private final InjectedValue<ExternalModuleService> injectedExternalModuleService = new InjectedValue<ExternalModuleService>();
    private final Bootstrap.Configuration configuration;
    private final BootstrapListener bootstrapListener;
    private final ControlledProcessState processState;

    /**
     * Construct a new instance.
     *
     * @param configuration the bootstrap configuration
     * @param prepareStep the prepare step to use
     */
    ServerService(final Bootstrap.Configuration configuration, final ControlledProcessState processState, final OperationStepHandler prepareStep, final BootstrapListener bootstrapListener) {
        super(OperationContext.Type.SERVER, configuration.getConfigurationPersister(), processState, ServerDescriptionProviders.ROOT_PROVIDER, prepareStep);
        this.configuration = configuration;
        this.bootstrapListener = bootstrapListener;
        this.processState = processState;
    }

    /**
     * Add this service to the given service target.
     *
     * @param serviceTarget the service target
     * @param configuration the bootstrap configuration
     */
    public static void addService(final ServiceTarget serviceTarget, final Bootstrap.Configuration configuration, final ControlledProcessState processState, final BootstrapListener bootstrapListener) {
        ServerService service = new ServerService(configuration, processState, null, bootstrapListener);
        ServiceBuilder<?> serviceBuilder = serviceTarget.addService(Services.JBOSS_SERVER_CONTROLLER, service);
        serviceBuilder.addDependency(ServerDeploymentRepository.SERVICE_NAME,ServerDeploymentRepository.class, service.injectedDeploymentRepository);
        serviceBuilder.addDependency(ContentRepository.SERVICE_NAME, ContentRepository.class, service.injectedContentRepository);
        serviceBuilder.addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ServiceModuleLoader.class, service.injectedModuleLoader);
        serviceBuilder.addDependency(Services.JBOSS_EXTERNAL_MODULE_SERVICE, ExternalModuleService.class,
                service.injectedExternalModuleService);
        serviceBuilder.install();
    }

    public void start(final StartContext context) throws StartException {
        super.start(context);
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
        DeployerChainAddHandler.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_STRUCTURE_DESCRIPTOR, new DeploymentStructureDescriptorParser());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_DEPENDENCIES_MANIFEST, new ManifestDependencyProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_COMPOSITE_ANNOTATION_INDEX, new CompositeIndexProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_ADDITIONAL_MODULES, new AdditionalModuleProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_CLASS_PATH, new ManifestClassPathProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EXTENSION_LIST, new ManifestExtensionListProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EXTENSION_NAME, new ManifestExtensionNameProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_SERVICE_LOADER_DEPLOYMENT, new ServiceLoaderProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_MODULE, new ModuleDependencyProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_SAR_MODULE, new ServiceActivatorDependencyProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_CLASS_PATH, new ModuleClassPathProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_EXTENSION_LIST, new ModuleExtensionListProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_SUB_DEPLOYMENTS, new SubDeploymentDependencyProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_JDK, new ServerDependenciesProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_MODULE_INFO_SERVICE, new ModuleInformationServiceProcessor());
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
            DeployerChainAddHandler.DEPLOYERS.set(null);
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
    }

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
        ServerControllerModelUtil.updateCoreModel(rootResource.getModel());
        ServerControllerModelUtil.initOperations(rootRegistration, injectedContentRepository.getValue(),
                configuration.getConfigurationPersister(), configuration.getServerEnvironment(), processState);

        // TODO maybe make creating of empty nodes part of the MNR description
        rootResource.registerChild(PathElement.pathElement(ModelDescriptionConstants.CORE_SERVICE, ModelDescriptionConstants.MANAGEMENT), Resource.Factory.create());
        rootResource.registerChild(PathElement.pathElement(ModelDescriptionConstants.CORE_SERVICE, ModelDescriptionConstants.SERVICE_CONTAINER), Resource.Factory.create());

        // Platform MBeans
        rootResource.registerChild(PlatformMBeanConstants.ROOT_PATH, new RootPlatformMBeanResource());
    }
}

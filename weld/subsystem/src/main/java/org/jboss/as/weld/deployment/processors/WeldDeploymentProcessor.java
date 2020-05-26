/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.weld.deployment.processors;

import static org.jboss.as.weld.util.Utils.putIfValueNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.enterprise.inject.spi.Extension;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.concurrent.ConcurrentContextSetupAction;
import org.jboss.as.ee.naming.JavaNamespaceSetup;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiNamingDependencyProcessor;
import org.jboss.as.naming.service.DefaultNamespaceContextSelectorService;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.server.Services;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.SetupAction;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.weld.ServiceNames;
import org.jboss.as.weld.WeldBootstrapService;
import org.jboss.as.weld._private.WeldDeploymentMarker;
import org.jboss.as.weld.WeldStartService;
import org.jboss.as.weld.deployment.BeanDeploymentArchiveImpl;
import org.jboss.as.weld.deployment.BeanDeploymentModule;
import org.jboss.as.weld.deployment.CdiAnnotationMarker;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.as.weld.deployment.WeldDeployment;
import org.jboss.as.weld.deployment.WeldPortableExtensions;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.as.weld.services.TCCLSingletonService;
import org.jboss.as.weld.services.bootstrap.WeldExecutorServices;
import org.jboss.as.weld.spi.BootstrapDependencyInstaller;
import org.jboss.as.weld.spi.DeploymentUnitDependenciesProvider;
import org.jboss.as.weld.spi.ModuleServicesProvider;
import org.jboss.as.weld.util.Reflections;
import org.jboss.as.weld.util.ServiceLoaders;
import org.jboss.as.weld.util.Utils;
import org.jboss.metadata.ear.spec.EarMetaData;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.bootstrap.spi.EEModuleDescriptor;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.config.ConfigurationKey;
import org.jboss.weld.configuration.spi.ExternalConfiguration;
import org.jboss.weld.configuration.spi.helpers.ExternalConfigurationBuilder;
import org.jboss.weld.manager.api.ExecutorServices;
import org.jboss.weld.security.spi.SecurityServices;
import org.jboss.weld.transaction.spi.TransactionServices;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Deployment processor that installs the weld services and all other required services
 *
 * @author Stuart Douglas
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class WeldDeploymentProcessor implements DeploymentUnitProcessor {

    private final boolean jtsEnabled;

    public WeldDeploymentProcessor(final boolean jtsEnabled) {
        this.jtsEnabled = jtsEnabled;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentUnit parent = Utils.getRootDeploymentUnit(deploymentUnit);
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        if (!WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {

            //if there are CDI annotation present and this is the top level deployment we log a warning
            if (deploymentUnit.getParent() == null && CdiAnnotationMarker.cdiAnnotationsPresent(deploymentUnit)) {
                WeldLogger.DEPLOYMENT_LOGGER.cdiAnnotationsButNotBeanArchive(deploymentUnit.getName());
            }
            return;
        }

        //add a dependency on the weld service to web deployments
        final ServiceName weldBootstrapServiceName = parent.getServiceName().append(WeldBootstrapService.SERVICE_NAME);
        ServiceName weldStartServiceName = parent.getServiceName().append(WeldStartService.SERVICE_NAME);
        deploymentUnit.addToAttachmentList(Attachments.WEB_DEPENDENCIES, weldStartServiceName);

        final Set<ServiceName> dependencies = new HashSet<ServiceName>();

        // we only start weld on top level deployments
        if (deploymentUnit.getParent() != null) {
            return;
        }

        WeldLogger.DEPLOYMENT_LOGGER.startingServicesForCDIDeployment(phaseContext.getDeploymentUnit().getName());

        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);

        final Set<BeanDeploymentArchiveImpl> beanDeploymentArchives = new HashSet<BeanDeploymentArchiveImpl>();
        final Map<ModuleIdentifier, BeanDeploymentModule> bdmsByIdentifier = new HashMap<ModuleIdentifier, BeanDeploymentModule>();
        final Map<ModuleIdentifier, ModuleSpecification> moduleSpecByIdentifier = new HashMap<ModuleIdentifier, ModuleSpecification>();
        final Map<ModuleIdentifier, EEModuleDescriptor> eeModuleDescriptors = new HashMap<>();

        // the root module only has access to itself. For most deployments this will be the only module
        // for ear deployments this represents the ear/lib directory.
        // war and jar deployment visibility will depend on the dependencies that
        // exist in the application, and mirror inter module dependencies
        final BeanDeploymentModule rootBeanDeploymentModule = deploymentUnit.getAttachment(WeldAttachments.BEAN_DEPLOYMENT_MODULE);
        putIfValueNotNull(eeModuleDescriptors, module.getIdentifier(), rootBeanDeploymentModule.getModuleDescriptor());

        bdmsByIdentifier.put(module.getIdentifier(), rootBeanDeploymentModule);

        moduleSpecByIdentifier.put(module.getIdentifier(), moduleSpecification);

        beanDeploymentArchives.addAll(rootBeanDeploymentModule.getBeanDeploymentArchives());
        final List<DeploymentUnit> subDeployments = deploymentUnit.getAttachmentList(Attachments.SUB_DEPLOYMENTS);

        final Set<ClassLoader> subDeploymentLoaders = new HashSet<ClassLoader>();

        final ServiceLoader<DeploymentUnitDependenciesProvider> dependenciesProviders = ServiceLoader.load(DeploymentUnitDependenciesProvider.class,
                WildFlySecurityManager.getClassLoaderPrivileged(WeldDeploymentProcessor.class));
        final ServiceLoader<ModuleServicesProvider> moduleServicesProviders = ServiceLoader.load(ModuleServicesProvider.class,
                WildFlySecurityManager.getClassLoaderPrivileged(WeldDeploymentProcessor.class));

        getDependencies(deploymentUnit, dependencies, dependenciesProviders);

        for (DeploymentUnit subDeployment : subDeployments) {
            getDependencies(subDeployment, dependencies, dependenciesProviders);
            final Module subDeploymentModule = subDeployment.getAttachment(Attachments.MODULE);
            if (subDeploymentModule == null) {
                continue;
            }
            subDeploymentLoaders.add(subDeploymentModule.getClassLoader());

            final ModuleSpecification subDeploymentModuleSpec = subDeployment.getAttachment(Attachments.MODULE_SPECIFICATION);
            final BeanDeploymentModule bdm = subDeployment.getAttachment(WeldAttachments.BEAN_DEPLOYMENT_MODULE);
            if (bdm == null) {
                continue;
            }
            // add the modules bdas to the global set of bdas
            beanDeploymentArchives.addAll(bdm.getBeanDeploymentArchives());
            bdmsByIdentifier.put(subDeploymentModule.getIdentifier(), bdm);
            moduleSpecByIdentifier.put(subDeploymentModule.getIdentifier(), subDeploymentModuleSpec);
            putIfValueNotNull(eeModuleDescriptors, subDeploymentModule.getIdentifier(), bdm.getModuleDescriptor());

            //we have to do this here as the aggregate components are not available in earlier phases
            final ResourceRoot subDeploymentRoot = subDeployment.getAttachment(Attachments.DEPLOYMENT_ROOT);

            // Add module services to bean deployment module
            for (Entry<Class<? extends Service>, Service> entry : ServiceLoaders.loadModuleServices(moduleServicesProviders, deploymentUnit, subDeployment, subDeploymentModule, subDeploymentRoot).entrySet()) {
                bdm.addService(entry.getKey(), Reflections.cast(entry.getValue()));
            }
        }

        for (Map.Entry<ModuleIdentifier, BeanDeploymentModule> entry : bdmsByIdentifier.entrySet()) {
            final ModuleSpecification bdmSpec = moduleSpecByIdentifier.get(entry.getKey());
            final BeanDeploymentModule bdm = entry.getValue();
            if (bdm == rootBeanDeploymentModule) {
                continue; // the root module only has access to itself
            }
            for (ModuleDependency dependency : bdmSpec.getSystemDependencies()) {
                BeanDeploymentModule other = bdmsByIdentifier.get(dependency.getIdentifier());
                if (other != null && other != bdm) {
                    bdm.addBeanDeploymentModule(other);
                }
            }
        }

        Map<Class<? extends Service>, Service> rootModuleServices = ServiceLoaders.loadModuleServices(moduleServicesProviders, deploymentUnit, deploymentUnit,
                module, deploymentRoot);

        // Add root module services to root bean deployment module
        for (Entry<Class<? extends Service>, Service> entry : rootModuleServices.entrySet()) {
            rootBeanDeploymentModule.addService(entry.getKey(), Reflections.cast(entry.getValue()));
        }

        // Add root module services to additional bean deployment archives
        for (final BeanDeploymentArchiveImpl additional : deploymentUnit.getAttachmentList(WeldAttachments.ADDITIONAL_BEAN_DEPLOYMENT_MODULES)) {
            beanDeploymentArchives.add(additional);
            for (Entry<Class<? extends Service>, Service> entry : rootModuleServices.entrySet()) {
                additional.getServices().add(entry.getKey(), Reflections.cast(entry.getValue()));
            }
        }

        final Collection<Metadata<Extension>> extensions = WeldPortableExtensions.getPortableExtensions(deploymentUnit).getExtensions();

        final WeldDeployment deployment = new WeldDeployment(beanDeploymentArchives, extensions, module, subDeploymentLoaders, deploymentUnit, rootBeanDeploymentModule, eeModuleDescriptors);

        installBootstrapConfigurationService(deployment, parent);

        // add the weld service
        final ServiceBuilder<?> weldBootstrapServiceBuilder = serviceTarget.addService(weldBootstrapServiceName);
        final Consumer<WeldBootstrapService> weldBootstrapServiceConsumer = weldBootstrapServiceBuilder.provides(weldBootstrapServiceName);
        weldBootstrapServiceBuilder.requires(TCCLSingletonService.SERVICE_NAME);
        final Supplier<ExecutorServices> executorServicesSupplier = weldBootstrapServiceBuilder.requires(WeldExecutorServices.SERVICE_NAME);
        final Supplier<ExecutorService> serverExecutorSupplier = weldBootstrapServiceBuilder.requires(Services.JBOSS_SERVER_EXECUTOR);
        Supplier<SecurityServices> securityServicesSupplier = null;
        Supplier<TransactionServices> weldTransactionServicesSupplier = null;

        // Install additional services
        final ServiceLoader<BootstrapDependencyInstaller> installers = ServiceLoader.load(BootstrapDependencyInstaller.class,
                WildFlySecurityManager.getClassLoaderPrivileged(WeldDeploymentProcessor.class));
        for (BootstrapDependencyInstaller installer : installers) {
            ServiceName serviceName = installer.install(serviceTarget, deploymentUnit, jtsEnabled);
            // Add dependency for recognized services
            if (ServiceNames.WELD_SECURITY_SERVICES_SERVICE_NAME.getSimpleName().equals(serviceName.getSimpleName())) {
                securityServicesSupplier = weldBootstrapServiceBuilder.requires(serviceName);
            } else if (ServiceNames.WELD_TRANSACTION_SERVICES_SERVICE_NAME.getSimpleName().equals(serviceName.getSimpleName())) {
                weldTransactionServicesSupplier = weldBootstrapServiceBuilder.requires(serviceName);
            }
        }
        final WeldBootstrapService weldBootstrapService = new WeldBootstrapService(deployment, WildFlyWeldEnvironment.INSTANCE, deploymentUnit.getName(),
                weldBootstrapServiceConsumer, executorServicesSupplier, serverExecutorSupplier, securityServicesSupplier, weldTransactionServicesSupplier);
        // Add root module services to WeldDeployment
        for (Entry<Class<? extends Service>, Service> entry : rootModuleServices.entrySet()) {
            weldBootstrapService.addWeldService(entry.getKey(), Reflections.cast(entry.getValue()));
        }
        weldBootstrapServiceBuilder.setInstance(weldBootstrapService);
        weldBootstrapServiceBuilder.install();

        final List<SetupAction> setupActions = getSetupActions(deploymentUnit);

        ServiceBuilder<?> startService = serviceTarget.addService(weldStartServiceName);
        for (final ServiceName dependency : dependencies) {
            startService.requires(dependency);
        }

        // make sure JNDI bindings are up
        startService.requires(JndiNamingDependencyProcessor.serviceName(deploymentUnit));

        final CapabilityServiceSupport capabilities = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        boolean tx = capabilities.hasCapability("org.wildfly.transactions");
        // [WFLY-5232]
        for (final ServiceName jndiSubsystemDependency : getJNDISubsytemDependencies(tx)) {
            startService.requires(jndiSubsystemDependency);
        }

        final EarMetaData earConfig = deploymentUnit.getAttachment(org.jboss.as.ee.structure.Attachments.EAR_METADATA);
        final List<String> startupBeanModules = deploymentUnit.getAttachmentList(org.jboss.as.ee.component.Attachments.DEPLOYMENT_STARTUP_MODULES);

        if (earConfig == null || (!earConfig.getInitializeInOrder() && (startupBeanModules.isEmpty())))  {
            // in-order install of sub-deployments may result in service dependencies deadlocks if the jndi dependency services of subdeployments are added as dependencies
            for (DeploymentUnit sub : subDeployments) {
                startService.requires(JndiNamingDependencyProcessor.serviceName(sub));
            }
        }
        final Supplier<WeldBootstrapService> bootstrapSupplier = startService.requires(weldBootstrapServiceName);
        startService.setInstance(new WeldStartService(bootstrapSupplier, setupActions, module.getClassLoader(), Utils.getRootDeploymentUnit(deploymentUnit).getServiceName()));
        startService.install();
    }

    private List<ServiceName> getJNDISubsytemDependencies(boolean tx) {
        List<ServiceName> dependencies = new ArrayList<>();
        if (tx) {
            dependencies.add(ContextNames.JBOSS_CONTEXT_SERVICE_NAME.append(ServiceName.of("UserTransaction")));
            dependencies.add(ContextNames.JBOSS_CONTEXT_SERVICE_NAME.append(ServiceName.of("TransactionSynchronizationRegistry")));
        }
        dependencies.add(NamingService.SERVICE_NAME);
        dependencies.add(DefaultNamespaceContextSelectorService.SERVICE_NAME);
        return dependencies;
    }

    private void installBootstrapConfigurationService(WeldDeployment deployment, DeploymentUnit parentDeploymentUnit) {
        final boolean nonPortableMode = parentDeploymentUnit.getAttachment(WeldConfiguration.ATTACHMENT_KEY).isNonPortableMode();
        final ExternalConfiguration configuration = new ExternalConfigurationBuilder()
            .add(ConfigurationKey.NON_PORTABLE_MODE.get(), nonPortableMode)
            .add(ConfigurationKey.ALLOW_OPTIMIZED_CLEANUP.get(), true)
            .build();
        deployment.getServices().add(ExternalConfiguration.class, configuration);
    }

    private void getDependencies(DeploymentUnit deploymentUnit, Set<ServiceName> dependencies, ServiceLoader<DeploymentUnitDependenciesProvider> providers) {
        for (DeploymentUnitDependenciesProvider provider : providers) {
            dependencies.addAll(provider.getDependencies(deploymentUnit));
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        final ServiceName weldTransactionServiceName = context.getServiceName().append(ServiceNames.WELD_TRANSACTION_SERVICES_SERVICE_NAME);
        final ServiceController<?> serviceController = context.getServiceRegistry().getService(weldTransactionServiceName);
        if (serviceController != null) {
            serviceController.setMode(ServiceController.Mode.REMOVE);
        }
    }

    static List<SetupAction> getSetupActions(DeploymentUnit deploymentUnit) {
        final List<SetupAction> setupActions = new ArrayList<SetupAction>();
        JavaNamespaceSetup naming = deploymentUnit.getAttachment(org.jboss.as.ee.naming.Attachments.JAVA_NAMESPACE_SETUP_ACTION);
        if (naming != null) {
            setupActions.add(naming);
        }
        final ConcurrentContextSetupAction concurrentContext = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.CONCURRENT_CONTEXT_SETUP_ACTION);
        if (concurrentContext != null) {
            setupActions.add(concurrentContext);
        }
        return setupActions;
    }
}


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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.enterprise.inject.spi.Extension;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.jboss.as.ee.component.EEApplicationDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.naming.JavaNamespaceSetup;
import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.as.jpa.config.Configuration;
import org.jboss.as.jpa.config.PersistenceUnitMetadataHolder;
import org.jboss.as.jpa.service.PersistenceUnitServiceImpl;
import org.jboss.as.naming.deployment.JndiNamingDependencyProcessor;
import org.jboss.as.security.service.SimpleSecurityManager;
import org.jboss.as.security.service.SimpleSecurityManagerService;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.SetupAction;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.txn.service.TransactionManagerService;
import org.jboss.as.txn.service.UserTransactionService;
import org.jboss.as.weld.WeldBootstrapService;
import org.jboss.as.weld.WeldStartService;
import org.jboss.as.weld.deployment.BeanDeploymentArchiveImpl;
import org.jboss.as.weld.deployment.BeanDeploymentModule;
import org.jboss.as.weld.deployment.CdiAnnotationMarker;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.as.weld.deployment.WeldDeployment;
import org.jboss.as.weld.deployment.WeldPortableExtensions;
import org.jboss.as.weld.discovery.WeldClassFileServices;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.as.weld.services.TCCLSingletonService;
import org.jboss.as.weld.services.bootstrap.WeldEjbInjectionServices;
import org.jboss.as.weld.services.bootstrap.WeldJaxwsInjectionServices;
import org.jboss.as.weld.services.bootstrap.WeldJpaInjectionServices;
import org.jboss.as.weld.services.bootstrap.WeldResourceInjectionServices;
import org.jboss.as.weld.services.bootstrap.WeldSecurityServices;
import org.jboss.as.weld.services.bootstrap.WeldTransactionServices;
import org.jboss.as.weld.util.Utils;
import org.jboss.metadata.ear.spec.EarMetaData;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.weld.bootstrap.api.Environments;
import org.jboss.weld.bootstrap.spi.BootstrapConfiguration;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.bootstrap.spi.helpers.FileBasedBootstrapConfiguration;
import org.jboss.weld.injection.spi.EjbInjectionServices;
import org.jboss.weld.injection.spi.JaxwsInjectionServices;
import org.jboss.weld.injection.spi.JpaInjectionServices;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.jboss.weld.resources.spi.ClassFileServices;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * Deployment processor that installs the weld services and all other required services
 *
 * @author Stuart Douglas
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
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
                WeldLogger.DEPLOYMENT_LOGGER.cdiAnnotationsButNotBeanArchive(deploymentUnit);
            }

            return;
        }

        //add a dependency on the weld service to web deployments
        final ServiceName weldBootstrapServiceName = parent.getServiceName().append(WeldBootstrapService.SERVICE_NAME);
        deploymentUnit.addToAttachmentList(Attachments.WEB_DEPENDENCIES, weldBootstrapServiceName);

        final Set<ServiceName> jpaServices = new HashSet<ServiceName>();


        // we only start weld on top level deployments
        if (deploymentUnit.getParent() != null) {
            return;
        }

        WeldLogger.DEPLOYMENT_LOGGER.startingServicesForCDIDeployment(phaseContext.getDeploymentUnit().getName());

        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final CompositeIndex rootIndex = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);

        final Set<BeanDeploymentArchiveImpl> beanDeploymentArchives = new HashSet<BeanDeploymentArchiveImpl>();
        final Map<ModuleIdentifier, BeanDeploymentModule> bdmsByIdentifier = new HashMap<ModuleIdentifier, BeanDeploymentModule>();
        final Map<ModuleIdentifier, ModuleSpecification> moduleSpecByIdentifier = new HashMap<ModuleIdentifier, ModuleSpecification>();

        // the root module only has access to itself. For most deployments this will be the only module
        // for ear deployments this represents the ear/lib directory.
        // war and jar deployment visibility will depend on the dependencies that
        // exist in the application, and mirror inter module dependencies
        final BeanDeploymentModule rootBeanDeploymentModule = deploymentUnit.getAttachment(WeldAttachments.BEAN_DEPLOYMENT_MODULE);

        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        final EEApplicationDescription eeApplicationDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_APPLICATION_DESCRIPTION);

        bdmsByIdentifier.put(module.getIdentifier(), rootBeanDeploymentModule);

        moduleSpecByIdentifier.put(module.getIdentifier(), moduleSpecification);

        beanDeploymentArchives.addAll(rootBeanDeploymentModule.getBeanDeploymentArchives());
        final List<DeploymentUnit> subDeployments = deploymentUnit.getAttachmentList(Attachments.SUB_DEPLOYMENTS);

        final Set<ClassLoader> subDeploymentLoaders = new HashSet<ClassLoader>();

        getJpaDependencies(deploymentUnit, jpaServices);

        for (DeploymentUnit subDeployment : subDeployments) {
            getJpaDependencies(deploymentUnit, jpaServices);
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

            //we have to do this here as the aggregate components are not available in earlier phases
            final ResourceRoot subDeploymentRoot = subDeployment.getAttachment(Attachments.DEPLOYMENT_ROOT);
            final EjbInjectionServices ejbInjectionServices = new WeldEjbInjectionServices(deploymentUnit.getServiceRegistry(), eeModuleDescription, eeApplicationDescription, subDeploymentRoot.getRoot());
            bdm.addService(EjbInjectionServices.class, ejbInjectionServices);

            final ResourceInjectionServices resourceInjectionServices = new WeldResourceInjectionServices(deploymentUnit.getServiceRegistry(), eeModuleDescription);
            bdm.addService(ResourceInjectionServices.class, resourceInjectionServices);

            final CompositeIndex index = subDeployment.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
            if (index != null) {
                bdm.addService(ClassFileServices.class, new WeldClassFileServices(index, subDeploymentModule.getClassLoader()));
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

        final EjbInjectionServices ejbInjectionServices = new WeldEjbInjectionServices(deploymentUnit.getServiceRegistry(),
                eeModuleDescription, eeApplicationDescription, deploymentRoot.getRoot());
        final ResourceInjectionServices resourceInjectionServices = new WeldResourceInjectionServices(deploymentUnit.getServiceRegistry(), eeModuleDescription);
        final WeldClassFileServices classFileServices = (rootIndex != null ? new WeldClassFileServices(rootIndex, module.getClassLoader()) : null);


        rootBeanDeploymentModule.addService(EjbInjectionServices.class, ejbInjectionServices);
        rootBeanDeploymentModule.addService(ResourceInjectionServices.class, resourceInjectionServices);
        if (classFileServices != null) {
            rootBeanDeploymentModule.addService(ClassFileServices.class, classFileServices);
        }

        for (final BeanDeploymentArchiveImpl additional : deploymentUnit.getAttachmentList(WeldAttachments.ADDITIONAL_BEAN_DEPLOYMENT_MODULES)) {
            beanDeploymentArchives.add(additional);
            additional.getServices().add(EjbInjectionServices.class, ejbInjectionServices);
            additional.getServices().add(ResourceInjectionServices.class, resourceInjectionServices);
            if (classFileServices != null) {
                additional.getServices().add(ClassFileServices.class, classFileServices);
            }
        }

        final Collection<Metadata<Extension>> extensions = WeldPortableExtensions.getPortableExtensions(deploymentUnit).getExtensions();

        final WeldDeployment deployment = new WeldDeployment(beanDeploymentArchives, extensions, module, subDeploymentLoaders, deploymentUnit, rootBeanDeploymentModule);

        final WeldBootstrapService weldBootstrapService = new WeldBootstrapService(deployment, Environments.EE_INJECT, deploymentUnit.getName());

        installBootstrapConfigurationService(deployment, parent);

        weldBootstrapService.addWeldService(EjbInjectionServices.class, ejbInjectionServices);
        weldBootstrapService.addWeldService(ResourceInjectionServices.class, resourceInjectionServices);

        if (classFileServices != null) {
            weldBootstrapService.addWeldService(ClassFileServices.class, classFileServices);
        }

        final JpaInjectionServices rootJpaInjectionServices = new WeldJpaInjectionServices(deploymentUnit);
        final JaxwsInjectionServices rootJaxWsInjectionServices = new WeldJaxwsInjectionServices(deploymentUnit);
        weldBootstrapService.addWeldService(JpaInjectionServices.class, rootJpaInjectionServices);
        weldBootstrapService.addWeldService(JaxwsInjectionServices.class, rootJaxWsInjectionServices);

        // add the weld service
        final ServiceBuilder<WeldBootstrapService> weldBootstrapServiceBuilder = serviceTarget.addService(weldBootstrapServiceName, weldBootstrapService);

        weldBootstrapServiceBuilder.addDependencies(TCCLSingletonService.SERVICE_NAME);

        installSecurityService(serviceTarget, deploymentUnit, weldBootstrapService, weldBootstrapServiceBuilder);
        installTransactionService(serviceTarget, deploymentUnit, weldBootstrapService, weldBootstrapServiceBuilder);

        weldBootstrapServiceBuilder.install();

        final List<SetupAction> setupActions = new ArrayList<SetupAction>();
        JavaNamespaceSetup naming = deploymentUnit.getAttachment(org.jboss.as.ee.naming.Attachments.JAVA_NAMESPACE_SETUP_ACTION);
        if (naming != null) {
            setupActions.add(naming);
        }

        final WeldStartService weldStartService = new WeldStartService(setupActions, module.getClassLoader(), Utils.getRootDeploymentUnit(deploymentUnit).getServiceName());

        ServiceBuilder<WeldStartService> startService = serviceTarget.addService(deploymentUnit.getServiceName().append(WeldStartService.SERVICE_NAME), weldStartService)
                .addDependency(weldBootstrapServiceName, WeldBootstrapService.class, weldStartService.getBootstrap())
                .addDependencies(jpaServices);

        // make sure JNDI bindings are up
        startService.addDependency(JndiNamingDependencyProcessor.serviceName(deploymentUnit));
        final EarMetaData earConfig = deploymentUnit.getAttachment(org.jboss.as.ee.structure.Attachments.EAR_METADATA);
        if (earConfig == null || !earConfig.getInitializeInOrder())  {
            // in-order install of sub-deployments may result in service dependencies deadlocks if the jndi dependency services of subdeployments are added as dependencies
            for (DeploymentUnit sub : subDeployments) {
                startService.addDependency(JndiNamingDependencyProcessor.serviceName(sub));
            }
        }

        startService.install();

    }

    private void getJpaDependencies(final DeploymentUnit deploymentUnit, final Set<ServiceName> jpaServices) {
        for (ResourceRoot root : DeploymentUtils.allResourceRoots(deploymentUnit)) {

            final PersistenceUnitMetadataHolder persistenceUnits = root.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS);
            if (persistenceUnits != null && persistenceUnits.getPersistenceUnits() != null) {
                for (final PersistenceUnitMetadata pu : persistenceUnits.getPersistenceUnits()) {
                    final Properties properties = pu.getProperties();
                    final String jpaContainerManaged = properties.getProperty(Configuration.JPA_CONTAINER_MANAGED);
                    final boolean deployPU = (jpaContainerManaged == null || Boolean.parseBoolean(jpaContainerManaged));
                    if (deployPU) {
                        final ServiceName serviceName = PersistenceUnitServiceImpl.getPUServiceName(pu);
                        jpaServices.add(serviceName);
                    }
                }
            }
        }
    }


    private ServiceName installSecurityService(ServiceTarget serviceTarget, DeploymentUnit deploymentUnit,
                                               WeldBootstrapService weldService, ServiceBuilder<WeldBootstrapService> weldServiceBuilder) {
        final WeldSecurityServices service = new WeldSecurityServices();

        final ServiceName serviceName = deploymentUnit.getServiceName().append(WeldSecurityServices.SERVICE_NAME);

        serviceTarget.addService(serviceName, service)
                .addDependency(ServiceBuilder.DependencyType.OPTIONAL, SimpleSecurityManagerService.SERVICE_NAME, SimpleSecurityManager.class, service.getSecurityManagerValue()).install();

        weldServiceBuilder.addDependency(serviceName, WeldSecurityServices.class, weldService.getSecurityServices());

        return serviceName;
    }

    private ServiceName installTransactionService(final ServiceTarget serviceTarget, final DeploymentUnit deploymentUnit,
                                                  WeldBootstrapService weldService, ServiceBuilder<WeldBootstrapService> weldServiceBuilder) {
        final WeldTransactionServices weldTransactionServices = new WeldTransactionServices(jtsEnabled);

        final ServiceName weldTransactionServiceName = deploymentUnit.getServiceName().append(
                WeldTransactionServices.SERVICE_NAME);

        serviceTarget.addService(weldTransactionServiceName, weldTransactionServices).addDependency(
                TransactionManagerService.SERVICE_NAME, TransactionManager.class,
                weldTransactionServices.getInjectedTransactionManager()).addDependency(UserTransactionService.SERVICE_NAME,
                UserTransaction.class, weldTransactionServices.getInjectedTransaction()).install();

        weldServiceBuilder.addDependency(weldTransactionServiceName, WeldTransactionServices.class, weldService
                .getWeldTransactionServices());

        return weldTransactionServiceName;
    }

    private void installBootstrapConfigurationService(WeldDeployment deployment, DeploymentUnit parentDeploymentUnit) {
        final boolean nonPortableMode = parentDeploymentUnit.getAttachment(WeldConfiguration.ATTACHMENT_KEY).isNonPortableMode();
        final ResourceLoader resourceLoader = deployment.getServices().get(ResourceLoader.class);
        deployment.getServices().add(BootstrapConfiguration.class, new FileBasedBootstrapConfiguration(resourceLoader) {
            @Override
            public boolean isNonPortableModeEnabled() {
                return nonPortableMode;
            }
        });
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        final ServiceName weldTransactionServiceName = context.getServiceName().append(WeldTransactionServices.SERVICE_NAME);
        final ServiceController<?> serviceController = context.getServiceRegistry().getService(weldTransactionServiceName);
        if (serviceController != null) {
            serviceController.setMode(ServiceController.Mode.REMOVE);
        }
    }

}

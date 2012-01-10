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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.Extension;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import javax.validation.ValidatorFactory;

import org.jboss.as.ee.beanvalidation.BeanValidationAttachments;
import org.jboss.as.ee.component.EEApplicationDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.jpa.config.PersistenceUnitMetadataHolder;
import org.jboss.as.jpa.service.PersistenceUnitServiceImpl;
import org.jboss.as.jpa.spi.PersistenceUnitMetadata;
import org.jboss.as.security.service.SimpleSecurityManager;
import org.jboss.as.security.service.SimpleSecurityManagerService;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.txn.service.TransactionManagerService;
import org.jboss.as.txn.service.UserTransactionService;
import org.jboss.as.weld.WeldContainer;
import org.jboss.as.weld.WeldDeploymentMarker;
import org.jboss.as.weld.WeldLogger;
import org.jboss.as.weld.deployment.BeanDeploymentArchiveImpl;
import org.jboss.as.weld.deployment.BeanDeploymentModule;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.as.weld.deployment.WeldDeployment;
import org.jboss.as.weld.services.TCCLSingletonService;
import org.jboss.as.weld.services.WeldService;
import org.jboss.as.weld.services.bootstrap.WeldEjbInjectionServices;
import org.jboss.as.weld.services.bootstrap.WeldEjbServices;
import org.jboss.as.weld.services.bootstrap.WeldJpaInjectionServices;
import org.jboss.as.weld.services.bootstrap.WeldResourceInjectionServices;
import org.jboss.as.weld.services.bootstrap.WeldSecurityServices;
import org.jboss.as.weld.services.bootstrap.WeldTransactionServices;
import org.jboss.as.weld.services.bootstrap.WeldValidationServices;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.weld.bootstrap.api.Environments;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.ejb.spi.EjbServices;
import org.jboss.weld.injection.spi.EjbInjectionServices;
import org.jboss.weld.injection.spi.JpaInjectionServices;
import org.jboss.weld.validation.spi.ValidationServices;

/**
 * Deployment processor that installs the weld services and all other required services
 *
 * @author Stuart Douglas
 */
public class WeldDeploymentProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentUnit parent = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        if (!WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
            return;
        }

        //add a dependency on the weld service to web deployments
        final ServiceName weldServiceName = parent.getServiceName().append(WeldService.SERVICE_NAME);
        deploymentUnit.addToAttachmentList(Attachments.WEB_DEPENDENCIES, weldServiceName);

        final Set<ServiceName> jpaServices = new HashSet<ServiceName>();


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

        final List<Metadata<Extension>> extensions = deploymentUnit.getAttachmentList(WeldAttachments.PORTABLE_EXTENSIONS);

        final WeldDeployment deployment = new WeldDeployment(beanDeploymentArchives, extensions, module, subDeploymentLoaders);

        final WeldContainer weldContainer = new WeldContainer(deployment, Environments.EE_INJECT);
        //hook up validation service
        //TODO: we need to change weld so this is a per-BDA service
        final ValidatorFactory factory = deploymentUnit.getAttachment(BeanValidationAttachments.VALIDATOR_FACTORY);
        weldContainer.addWeldService(ValidationServices.class, new WeldValidationServices(factory));

        final EjbInjectionServices ejbInjectionServices = new WeldEjbInjectionServices(deploymentUnit.getServiceRegistry(), eeModuleDescription, eeApplicationDescription, deploymentRoot.getRoot());
        weldContainer.addWeldService(EjbInjectionServices.class, ejbInjectionServices);
        rootBeanDeploymentModule.addService(EjbInjectionServices.class, ejbInjectionServices);

        weldContainer.addWeldService(EjbServices.class, new WeldEjbServices(deploymentUnit.getServiceRegistry()));


        final JpaInjectionServices rootJpaInjectionServices = new WeldJpaInjectionServices(deploymentUnit, deploymentUnit.getServiceRegistry());
        weldContainer.addWeldService(JpaInjectionServices.class, rootJpaInjectionServices);

        final WeldService weldService = new WeldService(weldContainer, deploymentUnit.getName());
        // add the weld service
        final ServiceBuilder<WeldContainer> weldServiceBuilder = serviceTarget.addService(weldServiceName, weldService);

        weldServiceBuilder.addDependencies(TCCLSingletonService.SERVICE_NAME);

        installResourceInjectionService(serviceTarget, deploymentUnit, weldService, weldServiceBuilder);
        installSecurityService(serviceTarget, deploymentUnit, weldService, weldServiceBuilder);
        installTransactionService(serviceTarget, deploymentUnit, weldService, weldServiceBuilder);

        weldServiceBuilder.addDependencies(jpaServices);
        weldServiceBuilder.install();

    }

    private void getJpaDependencies(final DeploymentUnit deploymentUnit, final Set<ServiceName> jpaServices) {
        for (ResourceRoot root : DeploymentUtils.allResourceRoots(deploymentUnit)) {

            final PersistenceUnitMetadataHolder persistenceUnits = root.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS);
            if (persistenceUnits != null && persistenceUnits.getPersistenceUnits() != null) {
                for (final PersistenceUnitMetadata pu : persistenceUnits.getPersistenceUnits()) {
                    final ServiceName serviceName = PersistenceUnitServiceImpl.getPUServiceName(pu);
                    jpaServices.add(serviceName);
                }
            }
        }
    }


    private ServiceName installSecurityService(ServiceTarget serviceTarget, DeploymentUnit deploymentUnit,
                                               WeldService weldService, ServiceBuilder<WeldContainer> weldServiceBuilder) {
        final WeldSecurityServices service = new WeldSecurityServices();

        final ServiceName serviceName = deploymentUnit.getServiceName().append(WeldSecurityServices.SERVICE_NAME);

        serviceTarget.addService(serviceName, service)
                .addDependency(ServiceBuilder.DependencyType.OPTIONAL, SimpleSecurityManagerService.SERVICE_NAME, SimpleSecurityManager.class, service.getSecurityManagerValue()).install();

        weldServiceBuilder.addDependency(serviceName, WeldSecurityServices.class, weldService.getSecurityServices());

        return serviceName;
    }

    private ServiceName installResourceInjectionService(ServiceTarget serviceTarget, DeploymentUnit deploymentUnit,
                                                        WeldService weldService, ServiceBuilder<WeldContainer> weldServiceBuilder) {
        final WeldResourceInjectionServices service = new WeldResourceInjectionServices();

        final ServiceName serviceName = deploymentUnit.getServiceName().append(WeldResourceInjectionServices.SERVICE_NAME);

        serviceTarget.addService(serviceName, service).install();

        weldServiceBuilder.addDependency(serviceName, WeldResourceInjectionServices.class, weldService
                .getResourceInjectionServices());

        return serviceName;
    }

    private ServiceName installTransactionService(final ServiceTarget serviceTarget, final DeploymentUnit deploymentUnit,
                                                  WeldService weldService, ServiceBuilder<WeldContainer> weldServiceBuilder) {
        final WeldTransactionServices weldTransactionServices = new WeldTransactionServices();

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

    @Override
    public void undeploy(DeploymentUnit context) {
        final ServiceName weldTransactionServiceName = context.getServiceName().append(WeldTransactionServices.SERVICE_NAME);
        final ServiceController<?> serviceController = context.getServiceRegistry().getService(weldTransactionServiceName);
        if (serviceController != null) {
            serviceController.setMode(ServiceController.Mode.REMOVE);
        }
    }

}

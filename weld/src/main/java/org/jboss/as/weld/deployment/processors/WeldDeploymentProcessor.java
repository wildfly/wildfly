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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.naming.Context;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.jboss.as.ee.naming.ContextServiceNameBuilder;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.txn.TransactionManagerService;
import org.jboss.as.txn.UserTransactionService;
import org.jboss.as.weld.WeldContainer;
import org.jboss.as.weld.WeldDeploymentMarker;
import org.jboss.as.weld.deployment.BeanDeploymentArchiveImpl;
import org.jboss.as.weld.deployment.WeldDeployment;
import org.jboss.as.weld.services.BeanManagerService;
import org.jboss.as.weld.services.WeldService;
import org.jboss.as.weld.services.bootstrap.WeldEjbInjectionServices;
import org.jboss.as.weld.services.bootstrap.WeldEjbServices;
import org.jboss.as.weld.services.bootstrap.WeldJpaInjectionServices;
import org.jboss.as.weld.services.bootstrap.WeldResourceInjectionServices;
import org.jboss.as.weld.services.bootstrap.WeldSecurityServices;
import org.jboss.as.weld.services.bootstrap.WeldTransactionServices;
import org.jboss.as.weld.services.bootstrap.WeldValidationServices;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.jboss.weld.bootstrap.api.Environments;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.util.ServiceLoader;

/**
 * Deployment processor that installs the weld services and all other required services
 *
 * @author Stuart Douglas
 *
 */
public class WeldDeploymentProcessor implements DeploymentUnitProcessor {

    private static final Logger log = Logger.getLogger("org.jboss.weld");

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();

        if (!WeldDeploymentMarker.isWeldDeployment(deploymentUnit)) {
            return;
        }
        // we only start weld on top level deployments
        if (deploymentUnit.getParent() != null) {
            return;
        }

        log.info("Starting Services for CDI deployment: " + phaseContext.getDeploymentUnit().getName());

        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);

        final List<BeanDeploymentArchiveImpl> beanDeploymentArchives = deploymentUnit
                .getAttachment(BeanDeploymentArchiveImpl.ATTACHMENT_KEY);
        deploymentUnit.removeAttachment(BeanDeploymentArchiveImpl.ATTACHMENT_KEY);

        // all bean deployment archives are accessible to each other
        // TODO: add proper accessibility rules
        for (BeanDeploymentArchiveImpl bda : beanDeploymentArchives) {
            bda.addBeanDeploymentArchives(beanDeploymentArchives);
        }

        // now load extensions
        final ServiceLoader<Extension> loader = ServiceLoader.load(Extension.class, module.getClassLoader());
        final HashSet<Metadata<Extension>> extensions = new HashSet<Metadata<Extension>>();
        final Iterator<Metadata<Extension>> iterator = loader.iterator();
        while (iterator.hasNext()) {
            Metadata<Extension> extension = iterator.next();
            extensions.add(extension);
        }

        final WeldDeployment deployment = new WeldDeployment(new HashSet<BeanDeploymentArchiveImpl>(beanDeploymentArchives),
                extensions, module);

        final WeldContainer weldContainer = new WeldContainer(deployment, Environments.EE_INJECT);

        final WeldService weldService = new WeldService(weldContainer);
        final ServiceName weldServiceName = deploymentUnit.getServiceName().append(WeldService.SERVICE_NAME);
        // add the weld service
        final ServiceBuilder<WeldContainer> weldServiceBuilder = serviceTarget.addService(weldServiceName, weldService);

        installEjbInjectionService(serviceTarget, deploymentUnit, weldService, weldServiceBuilder);
        installEjbService(serviceTarget, deploymentUnit, weldService, weldServiceBuilder);
        installJpaInjectionService(serviceTarget, deploymentUnit, weldService, weldServiceBuilder);
        installResourceInjectionService(serviceTarget, deploymentUnit, weldService, weldServiceBuilder);
        installSecurityService(serviceTarget, deploymentUnit, weldService, weldServiceBuilder);
        installTransactionService(serviceTarget, deploymentUnit, weldService, weldServiceBuilder);
        installValidationService(serviceTarget, deploymentUnit, weldService, weldServiceBuilder);

        weldServiceBuilder.install();

        // add the BeanManager service
        final ServiceName beanManagerServiceName = deploymentUnit.getServiceName().append(BeanManagerService.NAME);
        BeanManagerService beanManagerService = new BeanManagerService(deployment.getTopLevelBeanDeploymentArchive().getId());
        serviceTarget.addService(beanManagerServiceName, beanManagerService).addDependency(weldServiceName,
                WeldContainer.class, beanManagerService.getWeldContainer()).install();

        // bind the bean manager to JNDI
        final ServiceName moduleContextServiceName = ContextServiceNameBuilder.module(deploymentUnit);
        final ServiceName beanManagerBindingServiceName = moduleContextServiceName.append("BeanManager");

        InjectedValue<BeanManager> injectedBeanManager = new InjectedValue<BeanManager>();
        BinderService<BeanManager> beanManagerBindingService = new BinderService<BeanManager>("BeanManager",
                injectedBeanManager);
        serviceTarget.addService(beanManagerBindingServiceName, beanManagerBindingService).addDependency(
                moduleContextServiceName, Context.class, beanManagerBindingService.getContextInjector()).addDependency(
                beanManagerServiceName, BeanManager.class, injectedBeanManager).install();
    }

    private ServiceName installEjbInjectionService(ServiceTarget serviceTarget, DeploymentUnit deploymentUnit,
            WeldService weldService, ServiceBuilder<WeldContainer> weldServiceBuilder) {
        final WeldEjbInjectionServices service = new WeldEjbInjectionServices();

        final ServiceName serviceName = deploymentUnit.getServiceName().append(WeldEjbInjectionServices.SERVICE_NAME);

        serviceTarget.addService(serviceName, service).install();

        weldServiceBuilder.addDependency(serviceName, WeldEjbInjectionServices.class, weldService.getEjbInjectionServices());

        return serviceName;
    }

    private ServiceName installEjbService(ServiceTarget serviceTarget, DeploymentUnit deploymentUnit, WeldService weldService,
            ServiceBuilder<WeldContainer> weldServiceBuilder) {
        final WeldEjbServices service = new WeldEjbServices();

        final ServiceName serviceName = deploymentUnit.getServiceName().append(WeldEjbServices.SERVICE_NAME);

        serviceTarget.addService(serviceName, service).install();

        weldServiceBuilder.addDependency(serviceName, WeldEjbServices.class, weldService.getEjbServices());

        return serviceName;
    }

    private ServiceName installJpaInjectionService(ServiceTarget serviceTarget, DeploymentUnit deploymentUnit,
            WeldService weldService, ServiceBuilder<WeldContainer> weldServiceBuilder) {
        final WeldJpaInjectionServices service = new WeldJpaInjectionServices();

        final ServiceName serviceName = deploymentUnit.getServiceName().append(WeldJpaInjectionServices.SERVICE_NAME);

        serviceTarget.addService(serviceName, service).install();

        weldServiceBuilder.addDependency(serviceName, WeldJpaInjectionServices.class, weldService.getJpaInjectionServices());

        return serviceName;
    }

    private ServiceName installSecurityService(ServiceTarget serviceTarget, DeploymentUnit deploymentUnit,
            WeldService weldService, ServiceBuilder<WeldContainer> weldServiceBuilder) {
        final WeldSecurityServices service = new WeldSecurityServices();

        final ServiceName serviceName = deploymentUnit.getServiceName().append(WeldSecurityServices.SERVICE_NAME);

        serviceTarget.addService(serviceName, service).install();

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

    private ServiceName installValidationService(ServiceTarget serviceTarget, DeploymentUnit deploymentUnit,
            WeldService weldService, ServiceBuilder<WeldContainer> weldServiceBuilder) {
        final WeldValidationServices service = new WeldValidationServices();

        final ServiceName serviceName = deploymentUnit.getServiceName().append(WeldValidationServices.SERVICE_NAME);

        serviceTarget.addService(serviceName, service).install();

        weldServiceBuilder.addDependency(serviceName, WeldValidationServices.class, weldService.getValidationServices());

        return serviceName;
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

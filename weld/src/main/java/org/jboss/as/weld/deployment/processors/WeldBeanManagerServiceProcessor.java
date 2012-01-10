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

import java.util.Set;

import javax.enterprise.inject.spi.BeanManager;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentNamingMode;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.WeldContainer;
import org.jboss.as.weld.WeldDeploymentMarker;
import org.jboss.as.weld.WeldLogger;
import org.jboss.as.weld.arquillian.WeldContextSetup;
import org.jboss.as.weld.deployment.BeanDeploymentArchiveImpl;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.as.weld.services.BeanManagerService;
import org.jboss.as.weld.services.WeldService;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;

/**
 * {@link DeploymentUnitProcessor} that binds the bean manager to JNDI
 *
 * @author Stuart Douglas
 */
public class WeldBeanManagerServiceProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentUnit topLevelDeployment = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        if (!WeldDeploymentMarker.isPartOfWeldDeployment(topLevelDeployment)) {
            return;
        }

        final Set<ServiceName> dependencies = deploymentUnit.getAttachment(Attachments.JNDI_DEPENDENCIES);


        BeanDeploymentArchiveImpl rootBda = deploymentUnit
                .getAttachment(WeldAttachments.DEPLOYMENT_ROOT_BEAN_DEPLOYMENT_ARCHIVE);
        if (rootBda == null) {
            // this archive is not actually a bean archive.
            // then use the top level root bda
            rootBda = topLevelDeployment.getAttachment(WeldAttachments.DEPLOYMENT_ROOT_BEAN_DEPLOYMENT_ARCHIVE);
        }
        if (rootBda == null) {
            WeldLogger.ROOT_LOGGER.couldNotFindBeanManagerForDeployment(deploymentUnit.getName());
            return;
        }

        final ServiceName weldServiceName = topLevelDeployment.getServiceName().append(WeldService.SERVICE_NAME);

        // add the BeanManager service
        final ServiceName beanManagerServiceName = BeanManagerService.serviceName(deploymentUnit);
        BeanManagerService beanManagerService = new BeanManagerService(rootBda.getId());
        serviceTarget.addService(beanManagerServiceName, beanManagerService).addDependency(weldServiceName,
                WeldContainer.class, beanManagerService.getWeldContainer()).install();


        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);

        if (moduleDescription == null) {
            return;
        }

        //hack to set up a java:comp binding for jar deployments as well as wars
        if (DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit) || deploymentUnit.getName().endsWith(".jar")) {
            // bind the bean manager to JNDI
            final ServiceName moduleContextServiceName = ContextNames.contextServiceNameOfModule(moduleDescription.getApplicationName(), moduleDescription.getModuleName());
            bindBeanManager(serviceTarget, beanManagerServiceName, moduleContextServiceName, dependencies);
        }


        //bind the bm into java:comp for all components that require it
        for (ComponentDescription component : moduleDescription.getComponentDescriptions()) {
            if (component.getNamingMode() == ComponentNamingMode.CREATE) {
                final ServiceName compContextServiceName = ContextNames.contextServiceNameOfComponent(moduleDescription.getApplicationName(), moduleDescription.getModuleName(), component.getComponentName());
                bindBeanManager(serviceTarget, beanManagerServiceName, compContextServiceName, dependencies);
            }
        }
        deploymentUnit.addToAttachmentList(Attachments.SETUP_ACTIONS, new WeldContextSetup());
    }

    private void bindBeanManager(ServiceTarget serviceTarget, ServiceName beanManagerServiceName, ServiceName contextServiceName, final Set<ServiceName> dependencies) {
        final ServiceName beanManagerBindingServiceName = contextServiceName.append("BeanManager");
        dependencies.add(beanManagerBindingServiceName);
        InjectedValue<BeanManager> injectedBeanManager = new InjectedValue<BeanManager>();
        BinderService beanManagerBindingService = new BinderService("BeanManager");
        serviceTarget.addService(beanManagerBindingServiceName, beanManagerBindingService)
                .addInjection(beanManagerBindingService.getManagedObjectInjector(), new ValueManagedReferenceFactory(injectedBeanManager))
                .addDependency(contextServiceName, ServiceBasedNamingStore.class, beanManagerBindingService.getNamingStoreInjector())
                .addDependency(beanManagerServiceName, BeanManager.class, injectedBeanManager)
                .install();
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {


    }

}

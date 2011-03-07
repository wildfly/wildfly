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

package org.jboss.as.ee.naming;

import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.ValueManagedObject;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.Values;

/**
 * Deployment processor that deploys a naming context for the current module.
 *
 * @author John E. Bailey
 */
public class ModuleContextProcessor implements DeploymentUnitProcessor {

    /**
     * Add a ContextService for this module.
     *
     * @param phaseContext the deployment unit context
     * @throws org.jboss.as.server.deployment.DeploymentUnitProcessingException
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }
        EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();

        final ServiceName moduleContextServiceName = ContextNames.contextServiceNameOfModule(moduleDescription.getAppName(),moduleDescription.getModuleName());
        final RootContextService contextService = new RootContextService();
        serviceTarget.addService(moduleContextServiceName, contextService).install();

        final BinderService moduleNameBinder = new BinderService("ModuleName");

        serviceTarget.addService(moduleContextServiceName.append("ModuleName"), moduleNameBinder)
                .addInjection(moduleNameBinder.getManagedObjectInjector(), new ValueManagedObject(Values.immediateValue(moduleDescription.getModuleName())))
                .addDependency(moduleContextServiceName, NamingStore.class, moduleNameBinder.getNamingStoreInjector())
                .install();

        deploymentUnit.putAttachment(Attachments.MODULE_CONTEXT_CONFIG, moduleContextServiceName);

        // Add the namespace selector service for the module
        ServiceName appNs = ContextNames.contextServiceNameOfApplication(moduleDescription.getAppName());
        ServiceName namespaceSelectorServiceName = deploymentUnit.getServiceName().append(NamespaceSelectorService.NAME);
        NamespaceSelectorService namespaceSelector = new NamespaceSelectorService();
        serviceTarget.addService(namespaceSelectorServiceName, namespaceSelector)
                .addDependency(appNs, NamingStore.class, namespaceSelector.getApp())
                .addDependency(moduleContextServiceName, NamingStore.class, namespaceSelector.getModule())
                .addDependency(moduleContextServiceName, NamingStore.class, namespaceSelector.getComp())
                .install();

        // add the arquillian setup action, so the module namespace is available in arquillian tests
        JavaNamespaceSetup setupAction = new JavaNamespaceSetup(namespaceSelector);
        deploymentUnit.addToAttachmentList(org.jboss.as.server.deployment.Attachments.SETUP_ACTIONS, setupAction);
    }

    public void undeploy(DeploymentUnit context) {

    }
}

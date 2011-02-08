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

import javax.naming.Context;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.naming.service.ContextService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceController;
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
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();

        final ServiceName moduleContextServiceName = ContextServiceNameBuilder.module(deploymentUnit);
        final RootContextService contextService = new RootContextService();
        serviceTarget.addService(moduleContextServiceName, contextService).install();

        final BinderService<String> moduleNameBinder = new BinderService<String>("ModuleName", Values
                .immediateValue(getModuleName(deploymentUnit)));
        serviceTarget.addService(moduleContextServiceName.append("ModuleName"), moduleNameBinder).addDependency(
                moduleContextServiceName, Context.class, moduleNameBinder.getContextInjector()).install();

        final ContextService envContextService = new ContextService("env");
        serviceTarget.addService(moduleContextServiceName.append("env"), envContextService)
            .addDependency(moduleContextServiceName, Context.class, envContextService.getParentContextInjector())
            .install();

        // Create the module entry in the java:app/ context
        final ServiceName appContextServiceName = deploymentUnit.getAttachment(Attachments.APPLICATION_CONTEXT_CONFIG);
        if (appContextServiceName != null) {
            // sub deployments do not install this
            final ContextService appModuleContextService = new ContextService(deploymentUnit.getName());
            serviceTarget.addService(appContextServiceName.append(deploymentUnit.getName()), appModuleContextService)
                    .addDependency(appContextServiceName, Context.class, appModuleContextService.getParentContextInjector())
                    .install();
        }

        deploymentUnit.putAttachment(Attachments.MODULE_CONTEXT_CONFIG, moduleContextServiceName);

        // Add the namespace selector service for the module
        ServiceName appNs = ContextServiceNameBuilder.app(deploymentUnit);
        ServiceName namespaceSelectorServiceName = deploymentUnit.getServiceName().append(NamespaceSelectorService.NAME);
        NamespaceSelectorService namespaceSelector = new NamespaceSelectorService();
        serviceTarget.addService(namespaceSelectorServiceName, namespaceSelector).addDependency(appNs, Context.class,
                namespaceSelector.getApp()).addDependency(moduleContextServiceName, Context.class,
                namespaceSelector.getModule()).addDependency(moduleContextServiceName, Context.class,
                namespaceSelector.getComp()).install();

        // add the arquillian setup action, so the module namespace is availble in arquillian tests
        JavaNamespaceSetup setupAction = new JavaNamespaceSetup(namespaceSelector);
        deploymentUnit.addToAttachmentList(org.jboss.as.server.deployment.Attachments.SETUP_ACTIONS, setupAction);
    }

    private String getModuleName(final DeploymentUnit deploymentUnit) {
        final DeploymentUnit parent = deploymentUnit.getParent();
        if (parent != null && DeploymentTypeMarker.isType(DeploymentType.EAR, parent)) {
            return parent.getName() + "/" + deploymentUnit.getName();
        }
        return deploymentUnit.getName();
    }

    public void undeploy(DeploymentUnit context) {
        final ServiceName moduleContextServiceName = ContextServiceNameBuilder.module(context);
        final ServiceController<?> serviceController = context.getServiceRegistry().getService(moduleContextServiceName);
        if (serviceController != null) {
            serviceController.setMode(ServiceController.Mode.REMOVE);
        }
    }
}

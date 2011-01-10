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

import static org.jboss.as.ee.structure.EarDeploymentMarker.isEarDeployment;
import org.jboss.as.naming.deployment.ContextService;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
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
        if(isEarDeployment(deploymentUnit)) {
            return;
        }
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();

        final ServiceName parentContextName = getParentContextServiceName(deploymentUnit);
        final ServiceName moduleContextServiceName = parentContextName.append(deploymentUnit.getName());
        final JndiName moduleContextJndiName = getContextJndiName(deploymentUnit);

        final ContextService contextService = new ContextService(moduleContextJndiName);
        serviceTarget.addService(moduleContextServiceName, contextService)
            .addDependency(parentContextName, Context.class, contextService.getParentContextInjector())
            .install();

        final BinderService<String> moduleNameBinder = new BinderService<String>("ModuleName", Values.immediateValue(getModuleName(deploymentUnit)));
        serviceTarget.addService(moduleContextServiceName.append("module-name"), moduleNameBinder)
            .addDependency(moduleContextServiceName, Context.class, moduleNameBinder.getContextInjector())
            .install();

        phaseContext.getDeploymentUnit().putAttachment(Attachments.MODULE_CONTEXT_CONFIG, new NamingContextConfig(moduleContextServiceName, moduleContextJndiName));
    }

    private String getModuleName(final DeploymentUnit deploymentUnit) {
        final DeploymentUnit parent = deploymentUnit.getParent();
        if(parent != null && isEarDeployment(parent)) {
            return  parent.getName() + "/" + deploymentUnit.getName();
        }
        return deploymentUnit.getName();
    }

    private ServiceName getParentContextServiceName(final DeploymentUnit deploymentUnit) {
         final DeploymentUnit parent = deploymentUnit.getParent();
        if(parent != null && isEarDeployment(parent)) {
            return ContextNames.GLOBAL_CONTEXT_SERVICE_NAME.append(parent.getName());
        }
        return ContextNames.GLOBAL_CONTEXT_SERVICE_NAME;
    }

    private JndiName getContextJndiName(final DeploymentUnit deploymentUnit) {
         final DeploymentUnit parent = deploymentUnit.getParent();
        if(parent != null && isEarDeployment(parent)) {
            return ContextNames.GLOBAL_CONTEXT_NAME.append(parent.getName()).append(deploymentUnit.getName());
        }
        return ContextNames.GLOBAL_CONTEXT_NAME.append(deploymentUnit.getName());
    }

    public void undeploy(DeploymentUnit context) {
        final ServiceName moduleContextServiceName = ContextNames.GLOBAL_CONTEXT_SERVICE_NAME.append(context.getName());
        final ServiceController<?> serviceController = context.getServiceRegistry().getService(moduleContextServiceName);
        if(serviceController != null) {
            serviceController.setMode(ServiceController.Mode.REMOVE);
        }
    }
}

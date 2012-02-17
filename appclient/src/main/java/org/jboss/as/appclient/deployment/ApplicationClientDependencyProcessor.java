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
package org.jboss.as.appclient.deployment;

import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.moduleservice.ExtensionIndexService;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * DUP that handles app client dependencies.
 *
 * This DUP is quite unusual, as it will also remove dependencies if they refer to
 * dependencies that are not accessible to the application client. This allows a server
 * side deployment to reference another module, while still allowing the app client to
 * function when that additional deployment is not present.
 *
 * @author Stuart Douglas
 */
public class ApplicationClientDependencyProcessor implements DeploymentUnitProcessor {

    public static ModuleIdentifier CORBA_ID = ModuleIdentifier.create("org.omg.api");


    public ApplicationClientDependencyProcessor() {
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();


        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader loader = deploymentUnit.getAttachment(Attachments.SERVICE_MODULE_LOADER);

        moduleSpecification.addSystemDependency(new ModuleDependency(loader, CORBA_ID, false, true, true, false));

        final Set<ModuleIdentifier> moduleIdentifiers = new HashSet<ModuleIdentifier>();
        final DeploymentUnit top = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();

        moduleIdentifiers.add(top.getAttachment(Attachments.MODULE_IDENTIFIER));
        for(final DeploymentUnit module : top.getAttachmentList(Attachments.SUB_DEPLOYMENTS)) {
            moduleIdentifiers.add(module.getAttachment(Attachments.MODULE_IDENTIFIER));
        }

        final ListIterator<ModuleDependency> iterator = moduleSpecification.getMutableUserDependencies().listIterator();
        while (iterator.hasNext()) {
            final ModuleDependency dep = iterator.next();
            final ModuleIdentifier identifier = dep.getIdentifier();
            if(identifier.getName().startsWith(ServiceModuleLoader.MODULE_PREFIX) &&
                   !identifier.getName().startsWith(ExtensionIndexService.MODULE_PREFIX)) {
                if(!moduleIdentifiers.contains(identifier)) {
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}

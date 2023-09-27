/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

    public static final ModuleIdentifier CORBA_ID = ModuleIdentifier.create("org.omg.api");
    public static final ModuleIdentifier XNIO = ModuleIdentifier.create("org.jboss.xnio");


    public ApplicationClientDependencyProcessor() {
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();


        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader loader = deploymentUnit.getAttachment(Attachments.SERVICE_MODULE_LOADER);

        moduleSpecification.addSystemDependency(new ModuleDependency(loader, CORBA_ID, false, true, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(loader, XNIO, false, true, true, false));

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
            if (identifier.getName().startsWith(ServiceModuleLoader.MODULE_PREFIX)
                    && !identifier.getName().startsWith(ExtensionIndexService.MODULE_PREFIX)
                    && !moduleIdentifiers.contains(identifier)) {
                iterator.remove();
            }
        }
    }

}

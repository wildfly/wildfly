/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.appclient.deployment;

import java.util.HashSet;
import java.util.Set;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.moduleservice.ExtensionIndexService;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
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

    public static final String CORBA_ID = "org.omg.api";
    public static final String XNIO = "org.jboss.xnio";


    public ApplicationClientDependencyProcessor() {
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();


        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader loader = deploymentUnit.getAttachment(Attachments.SERVICE_MODULE_LOADER);

        moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(loader, CORBA_ID).setExport(true).setImportServices(true).build());
        moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(loader, XNIO).setExport(true).setImportServices(true).build());

        final Set<String> moduleIdentifiers = new HashSet<>();
        final DeploymentUnit top = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();

        moduleIdentifiers.add(top.getAttachment(Attachments.MODULE_NAME));
        for(final DeploymentUnit module : top.getAttachmentList(Attachments.SUB_DEPLOYMENTS)) {
            moduleIdentifiers.add(module.getAttachment(Attachments.MODULE_NAME));
        }

        moduleSpecification.removeUserDependencies(dep -> {
            final String identifier = dep.getDependencyModule();
            return identifier.startsWith(ServiceModuleLoader.MODULE_PREFIX)
                    && !identifier.startsWith(ExtensionIndexService.MODULE_PREFIX)
                    && !moduleIdentifiers.contains(identifier);
        });
    }

}

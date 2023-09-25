/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.iiop.openjdk.deployment;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * @author Stuart Douglas
 */
public class IIOPDependencyProcessor implements DeploymentUnitProcessor {
    public static final ModuleIdentifier CORBA_ID = ModuleIdentifier.create("org.omg.api");
    public static final ModuleIdentifier JAVAX_RMI_API_ID = ModuleIdentifier.create("javax.rmi.api");
    public static final ModuleIdentifier IIOP_OPENJDK_ID = ModuleIdentifier.create("javax.orb.api");

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);

        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, CORBA_ID, false, false, false, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, JAVAX_RMI_API_ID, false, false, false, false));
        //we need to add iiop, as the orb is initialized from the context class loader of the deployment
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, IIOP_OPENJDK_ID, false, false, false, false));
    }
}

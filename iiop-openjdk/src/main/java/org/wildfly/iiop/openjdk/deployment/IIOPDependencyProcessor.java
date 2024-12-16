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
import org.jboss.modules.ModuleLoader;

/**
 * @author Stuart Douglas
 */
public class IIOPDependencyProcessor implements DeploymentUnitProcessor {
    public static final String CORBA_ID = "org.omg.api";
    public static final String JAVAX_RMI_API_ID = "javax.rmi.api";
    public static final String IIOP_OPENJDK_ID = "javax.orb.api";

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

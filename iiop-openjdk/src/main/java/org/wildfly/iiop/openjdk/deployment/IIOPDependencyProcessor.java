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

    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}

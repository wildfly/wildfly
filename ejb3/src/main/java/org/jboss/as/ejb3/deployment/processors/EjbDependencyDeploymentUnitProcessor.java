/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.jacorb.deployment.JacORBDeploymentMarker;
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

import static org.jboss.as.ejb3.deployment.EjbDeploymentMarker.isEjbDeployment;

/**
 * Responsible for adding appropriate Java EE {@link org.jboss.as.server.deployment.module.ModuleDependency module dependencies}
 * <p/>
 * Author : Jaikiran Pai
 */
public class EjbDependencyDeploymentUnitProcessor implements DeploymentUnitProcessor {

    // TODO: This should be centralized some place
    /**
     * Module id for Java EE module
     */
    private static final ModuleIdentifier JAVAEE_MODULE_IDENTIFIER = ModuleIdentifier.create("javaee.api");

    /**
     * Needed for timer handle persistence
     * TODO: restrict visibility
     */
    private static final ModuleIdentifier EJB_SUBSYSTEM = ModuleIdentifier.create("org.jboss.as.ejb3");
    private static final ModuleIdentifier EJB_CLIENT = ModuleIdentifier.create("org.jboss.ejb-client");
    private static final ModuleIdentifier EJB_IIOP_CLIENT = ModuleIdentifier.create("org.jboss.iiop-client");
    private static final ModuleIdentifier JACORB = ModuleIdentifier.create("org.jboss.as.jacorb");


    /**
     * Adds Java EE module as a dependency to any deployment unit which is a EJB deployment
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     *
     */
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {


        // get hold of the deployment unit
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);

        //we always give them the EJB client
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, EJB_CLIENT, false, false, false, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, EJB_IIOP_CLIENT, false, false, false, false));

        //we always have to add this, as even non-ejb deployments may still lookup IIOP ejb's
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, EJB_SUBSYSTEM, false, false, false, false));

        if (JacORBDeploymentMarker.isJacORBDeployment(deploymentUnit)) {
            //needed for dynamic IIOP stubs
            moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, JACORB, false, false, false, false));
        }

        // fetch the EjbJarMetaData
        //TODO: remove the app client bit after the next EJB release
        if (!isEjbDeployment(deploymentUnit) && !DeploymentTypeMarker.isType(DeploymentType.APPLICATION_CLIENT, deploymentUnit)) {
            // nothing to do
            return;
        }


        // FIXME: still not the best way to do it
        //this must be the first dep listed in the module
        if (Boolean.getBoolean("org.jboss.as.ejb3.EMBEDDED"))
            moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, ModuleIdentifier.CLASSPATH, false, false, false, false));

        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, JAVAEE_MODULE_IDENTIFIER, false, false, false, false));

    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}

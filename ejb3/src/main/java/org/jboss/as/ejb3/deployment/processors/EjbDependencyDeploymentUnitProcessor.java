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

import static org.jboss.as.server.deployment.EjbDeploymentMarker.isEjbDeployment;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
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
import org.wildfly.iiop.openjdk.deployment.IIOPDeploymentMarker;

/**
 * Responsible for adding appropriate Java EE {@link org.jboss.as.server.deployment.module.ModuleDependency module dependencies}
 * <p/>
 * Author : Jaikiran Pai
 */
public class EjbDependencyDeploymentUnitProcessor implements DeploymentUnitProcessor {

    /**
     * Needed for timer handle persistence
     * TODO: restrict visibility
     */
    private static final ModuleIdentifier EJB_SUBSYSTEM = ModuleIdentifier.create("org.jboss.as.ejb3");
    private static final ModuleIdentifier EJB_CLIENT = ModuleIdentifier.create("org.jboss.ejb-client");
    private static final ModuleIdentifier EJB_NAMING_CLIENT = ModuleIdentifier.create("org.wildfly.naming-client");
    private static final ModuleIdentifier EJB_IIOP_CLIENT = ModuleIdentifier.create("org.jboss.iiop-client");
    private static final ModuleIdentifier IIOP_OPENJDK = ModuleIdentifier.create("org.wildfly.iiop-openjdk");
    private static final ModuleIdentifier EJB_API = ModuleIdentifier.create("javax.ejb.api");
    private static final ModuleIdentifier JAX_RPC_API = ModuleIdentifier.create("javax.xml.rpc.api");
    private static final ModuleIdentifier HTTP_EJB = ModuleIdentifier.create("org.wildfly.http-client.ejb");
    private static final ModuleIdentifier HTTP_TRANSACTION = ModuleIdentifier.create("org.wildfly.http-client.transaction");
    private static final ModuleIdentifier HTTP_NAMING = ModuleIdentifier.create("org.wildfly.http-client.naming");


    /**
     * Adds Java EE module as a dependency to any deployment unit which is an EJB deployment
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

        //always add EE API
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, EJB_API, false, false, true, false));
        // previously exported by EJB_API prior to WFLY-5922 TODO WFLY-5967 look into moving this to WS subsystem
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, JAX_RPC_API, false, false, true, false));
        //we always give them the EJB client
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, EJB_CLIENT, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, EJB_NAMING_CLIENT, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, EJB_IIOP_CLIENT, false, false, false, false));

        //we always have to add this, as even non-ejb deployments may still lookup IIOP ejb's
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, EJB_SUBSYSTEM, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, HTTP_EJB, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, HTTP_NAMING, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, HTTP_TRANSACTION, false, false, true, false));

        if (IIOPDeploymentMarker.isIIOPDeployment(deploymentUnit)) {
            //needed for dynamic IIOP stubs
            moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, IIOP_OPENJDK, false, false, false, false));
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

    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}

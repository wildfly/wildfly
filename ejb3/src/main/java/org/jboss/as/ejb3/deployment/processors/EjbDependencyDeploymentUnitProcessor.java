/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
import org.jboss.modules.ModuleLoader;
import org.wildfly.iiop.openjdk.deployment.IIOPDeploymentMarker;

/**
 * Responsible for adding appropriate Jakarta EE {@link org.jboss.as.server.deployment.module.ModuleDependency module dependencies}
 *
 * @author Jaikiran Pai
 */
public class EjbDependencyDeploymentUnitProcessor implements DeploymentUnitProcessor {

    /**
     * Needed for timer handle persistence
     * TODO: restrict visibility
     */
    private static final String EJB_SUBSYSTEM = "org.jboss.as.ejb3";
    private static final String EJB_CLIENT = "org.jboss.ejb-client";
    private static final String EJB_NAMING_CLIENT = "org.wildfly.naming-client";
    private static final String EJB_IIOP_CLIENT = "org.jboss.iiop-client";
    private static final String IIOP_OPENJDK = "org.wildfly.iiop-openjdk";
    private static final String EJB_API = "jakarta.ejb.api";
    private static final String HTTP_EJB = "org.wildfly.http-client.ejb";
    private static final String HTTP_TRANSACTION = "org.wildfly.http-client.transaction";
    private static final String HTTP_NAMING = "org.wildfly.http-client.naming";
    private static final String CLUSTERING_EJB_CLIENT = "org.wildfly.clustering.ejb.client";

    /**
     * Adds Jakarta EE module as a dependency to any deployment unit which is a Jakarta Enterprise Beans deployment
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
        moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, EJB_API).setImportServices(true).build());
        //we always give them the Jakarta Enterprise Beans client
        moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, EJB_CLIENT).setImportServices(true).build());
        moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, EJB_NAMING_CLIENT).setImportServices(true).build());
        moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, EJB_IIOP_CLIENT).build());

        //we always have to add this, as even non-ejb deployments may still lookup IIOP ejb's
        moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, EJB_SUBSYSTEM).setImportServices(true).build());
        moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, HTTP_EJB).setImportServices(true).build());
        moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, HTTP_NAMING).setImportServices(true).build());
        moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, HTTP_TRANSACTION).setImportServices(true).build());
        // Marshalling support for EJB SessionIDs
        // TODO Move this to distributable-ejb subsystem
        moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, CLUSTERING_EJB_CLIENT).setImportServices(true).build());

        if (IIOPDeploymentMarker.isIIOPDeployment(deploymentUnit)) {
            //needed for dynamic IIOP stubs
            moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, IIOP_OPENJDK).build());
        }

        // fetch the EjbJarMetaData
        //TODO: remove the app client bit after the next Jakarta Enterprise Beans release
        if (!isEjbDeployment(deploymentUnit) && !DeploymentTypeMarker.isType(DeploymentType.APPLICATION_CLIENT, deploymentUnit)) {
            // nothing to do
            return;
        }


        // FIXME: still not the best way to do it
        //this must be the first dep listed in the module
        if (Boolean.getBoolean("org.jboss.as.ejb3.EMBEDDED"))
            moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, "Classpath").build());

    }
}

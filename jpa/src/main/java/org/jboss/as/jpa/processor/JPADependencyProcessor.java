/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jpa.processor;

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
 * Deployment processor which adds a module dependencies for modules needed for JPA deployments.
 *
 * @author Scott Marlow (copied from WeldDepedencyProcessor)
 */
public class JPADependencyProcessor implements DeploymentUnitProcessor {

    private static ModuleIdentifier JAVAX_PERSISTENCE_API_ID = ModuleIdentifier.create("javax.persistence.api");
    private static ModuleIdentifier JAVAEE_API_ID = ModuleIdentifier.create("javaee.api");
    private static ModuleIdentifier JBOSS_AS_JPA_ID = ModuleIdentifier.create("org.jboss.as.jpa");
    private static ModuleIdentifier JBOSS_HIBERNATE_ID = ModuleIdentifier.create("org.hibernate");
    private static ModuleIdentifier JAVASSIST_ID = ModuleIdentifier.create("org.javassist");


    /**
     * Add dependencies for modules required for JPA deployments
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        addDependency(moduleSpecification, moduleLoader, JAVAX_PERSISTENCE_API_ID);


        // TODO:  (JBAS-9034) also check if the deployment uses JPA (has @PersistenceUnit, @PersistenceContext or persistence.xml)
        if (!JPADeploymentMarker.isJPADeployment(deploymentUnit)) {
            return; // Skip if there are no persistence use in the deployment
        }
        addDependency(moduleSpecification, moduleLoader, JAVAEE_API_ID);
        addDependency(moduleSpecification, moduleLoader, JBOSS_AS_JPA_ID);
        addDependency(moduleSpecification, moduleLoader, JBOSS_HIBERNATE_ID);
        addDependency(moduleSpecification, moduleLoader, JAVASSIST_ID);
    }

    private void addDependency(ModuleSpecification moduleSpecification, ModuleLoader moduleLoader,
                               ModuleIdentifier moduleIdentifier) {
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, moduleIdentifier, false, false, false));
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}

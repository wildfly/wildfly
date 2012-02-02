/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.managedbean.processors;

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
import org.jboss.modules.filter.PathFilters;

/**
 * Deployment processor which adds the java EE APIs to EE deployments
 * <p/>
 * TODO: This needs to be removed, so that the relevant API's are added by the corresponding sub systems
 *
 * @author John E. Bailey
 * @author Jason T. Greene
 * @author Stuart Douglas
 */
public class JavaEEDependencyProcessor implements DeploymentUnitProcessor {

    private static ModuleIdentifier JAVAEE_API_ID = ModuleIdentifier.create("javaee.api");

    private static ModuleIdentifier HIBERNATE_VALIDATOR_ID = ModuleIdentifier.create("org.hibernate.validator");

    private static ModuleIdentifier JBOSS_INVOCATION_ID = ModuleIdentifier.create("org.jboss.invocation");
    private static ModuleIdentifier JBOSS_AS_EE = ModuleIdentifier.create("org.jboss.as.ee");


    /**
     * Add the EE APIs as a dependency to all deployments
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     *
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);

        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, JAVAEE_API_ID, false, false, true, false));
        // TODO: Post 7.0, we have to rethink this whole hibernate dependencies that we add to user deployments
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, HIBERNATE_VALIDATOR_ID, false, false, true, false));

        //add jboss-invocation classes needed by the proxies
        ModuleDependency invocation = new ModuleDependency(moduleLoader, JBOSS_INVOCATION_ID, false, false, false, false);
        invocation.addImportFilter(PathFilters.is("org.jboss.invocation.proxy.classloading"), true);
        moduleSpecification.addSystemDependency(invocation);

        ModuleDependency ee = new ModuleDependency(moduleLoader, JBOSS_AS_EE, false, false, false, false);
        ee.addImportFilter(PathFilters.is("org.jboss.as.ee.component.serialization"), true);
        moduleSpecification.addSystemDependency(ee);
    }

    public void undeploy(final DeploymentUnit context) {
    }
}

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

package org.jboss.as.security.deployment;

import org.jboss.as.security.ModuleName;
import org.jboss.as.security.remoting.RemotingLoginModule;
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
 * Adds a security subsystem dependency to deployments
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jan 14, 2011
 */
public class SecurityDependencyProcessor implements DeploymentUnitProcessor {

    public static final ModuleIdentifier PICKETBOX_ID = ModuleIdentifier.create(ModuleName.PICKETBOX.getName(),
            ModuleName.PICKETBOX.getSlot());
    public static final ModuleIdentifier REMOTING_LOGIN_MODULE = ModuleIdentifier.create("org.jboss.as.security");

    public static final ModuleIdentifier AUTH_MESSAGE_API = ModuleIdentifier.create("javax.security.auth.message.api");
    public static final ModuleIdentifier JACC_API = ModuleIdentifier.create("javax.security.jacc.api");

    /** {@inheritDoc} */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, PICKETBOX_ID, false, false, false, false));

        //add the remoting login module
        final ModuleDependency remoting = new ModuleDependency(moduleLoader, REMOTING_LOGIN_MODULE, false, false, false, false);
        remoting.addImportFilter(PathFilters.is(RemotingLoginModule.class.getName().replace(".","/")), true);
        moduleSpecification.addSystemDependency(remoting);


        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, JACC_API, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, AUTH_MESSAGE_API, false, false, true, false));
    }

    /** {@inheritDoc} */
    public void undeploy(DeploymentUnit context) {
    }

}
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
package org.jboss.as.web.deployment;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import static org.jboss.as.web.deployment.WarDeploymentMarker.isWarDeployment;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * Module dependencies processor.
 *
 * @author Emanuel Muckenhuber
 */
public class WarClassloadingDependencyProcessor implements DeploymentUnitProcessor {

    private static final ModuleIdentifier JAVAEE_API_ID = ModuleIdentifier.create("javaee.api");
    private static final ModuleIdentifier JAVAX_SERVLET_API = ModuleIdentifier.create("javax.servlet.api");
    private static final ModuleIdentifier JAVAX_SERVLET_JSP_API = ModuleIdentifier.create("javax.servlet.jsp.api");
    private static final ModuleIdentifier JBOSS_WEB = ModuleIdentifier.create("org.jboss.as.web");
    private static final ModuleIdentifier SYSTEM = ModuleIdentifier.create("system");
    private static final ModuleIdentifier LOG = ModuleIdentifier.create("org.jboss.logging");

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if(!isWarDeployment(deploymentUnit)) {
            return; // Skip non web deployments
        }
        final ModuleLoader moduleLoader = Module.getSystemModuleLoader();
        // Add module dependencies on Java EE apis

        deploymentUnit.addToAttachmentList(Attachments.MODULE_DEPENDENCIES, new ModuleDependency(moduleLoader, JAVAX_SERVLET_API, false, false, false));
        deploymentUnit.addToAttachmentList(Attachments.MODULE_DEPENDENCIES, new ModuleDependency(moduleLoader, JAVAX_SERVLET_API, false, false, false));
        deploymentUnit.addToAttachmentList(Attachments.MODULE_DEPENDENCIES, new ModuleDependency(moduleLoader, JAVAX_SERVLET_JSP_API, false, false, false));

        // FIXME we need to revise the exports of the web module, so that we
        // don't export our internals
        deploymentUnit.addToAttachmentList(Attachments.MODULE_DEPENDENCIES, new ModuleDependency(moduleLoader, JBOSS_WEB, false, false, false));
        // JFC hack...
        deploymentUnit.addToAttachmentList(Attachments.MODULE_DEPENDENCIES, new ModuleDependency(moduleLoader, SYSTEM, false, false, false));
        deploymentUnit.addToAttachmentList(Attachments.MODULE_DEPENDENCIES, new ModuleDependency(moduleLoader, LOG, false, false, false));
    }

    public void undeploy(final DeploymentUnit context) {
    }
}

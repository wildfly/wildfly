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

package org.jboss.as.ee.component.deployers;

import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class that is responsible for resolving name conflicts.
 *
 * //TODO: this must be able to deal with the case of module names being changed via deployment descriptor
 *
 * @author Stuart Douglas
 */
public final class EEModuleNameProcessor implements DeploymentUnitProcessor {

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final List<DeploymentUnit> subDeployments = deploymentUnit.getAttachmentList(Attachments.SUB_DEPLOYMENTS);
        final Set<String> moduleNames = new HashSet<String>();
        final Set<String> moduleConflicts = new HashSet<String>();
        //look for modules with the same name
        //
        for(DeploymentUnit deployment : subDeployments) {
            final EEModuleDescription module = deployment.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
            if(module != null) {
                if(moduleNames.contains(module.getModuleName())) {
                    moduleConflicts.add(module.getModuleName());
                }
                moduleNames.add(module.getModuleName());
            }
        }
        for(DeploymentUnit deployment : subDeployments) {
            final EEModuleDescription module = deployment.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
            if(module != null) {
                if(moduleConflicts.contains(module.getModuleName())) {
                    module.setModuleName(deployment.getName());
                }
            }
        }

    }

    public void undeploy(final DeploymentUnit context) {
    }
}

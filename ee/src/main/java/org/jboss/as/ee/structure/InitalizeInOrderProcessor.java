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
package org.jboss.as.ee.structure;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.Services;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.metadata.ear.spec.Ear6xMetaData;
import org.jboss.metadata.ear.spec.EarMetaData;
import org.jboss.metadata.ear.spec.ModuleMetaData;
import org.jboss.msc.service.ServiceName;

/**
 * Sets up dependencies for the next phase if initialize in order is set.
 *
 * @author Stuart Douglas
 */
public class InitalizeInOrderProcessor implements DeploymentUnitProcessor{
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if(deploymentUnit.getParent() == null) {
            return;
        }
        final EarMetaData earConfig = deploymentUnit.getParent().getAttachment(Attachments.EAR_METADATA);
        if(earConfig != null) {
            if(earConfig instanceof Ear6xMetaData) {
                boolean inOrder=((Ear6xMetaData) earConfig).getInitializeInOrder();
                if(inOrder) {
                    final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT);
                    ModuleMetaData thisModulesMetadata = deploymentRoot.getAttachment(Attachments.MODULE_META_DATA);
                    if(thisModulesMetadata != null && thisModulesMetadata.getType() != ModuleMetaData.ModuleType.Client) {
                        ModuleMetaData previous = null;
                        for(ModuleMetaData module : earConfig.getModules()) {
                            if(module.getFileName().equals(thisModulesMetadata.getFileName())) {
                                break;
                            }
                        }
                        if(previous != null) {
                            final ServiceName serviceName = Services.deploymentUnitName(deploymentUnit.getParent().getName(), previous.getFileName());
                            phaseContext.addToAttachmentList(org.jboss.as.server.deployment.Attachments.NEXT_PHASE_DEPS,serviceName.append(Phase.INSTALL.name()));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}

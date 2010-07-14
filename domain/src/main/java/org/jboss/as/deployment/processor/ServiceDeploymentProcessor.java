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

package org.jboss.as.deployment.processor;

import org.jboss.as.deployment.DeploymentPhases;
import org.jboss.as.deployment.item.ServiceDeployment;
import org.jboss.as.deployment.item.ServiceDeploymentItem;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.vfs.VirtualFile;

import static org.jboss.as.deployment.attachment.VirtualFileAttachment.getVirtualFileAttachment;

/**
 * DeploymentUnitProcessor that detects ServiceDeployment instances for a deployment and creates the appropriate
 * DeploymentItem instances to execute the deployments.
 *
 * @author John E. Bailey
 */
public class ServiceDeploymentProcessor implements DeploymentUnitProcessor {
    public static final long PRIORITY = DeploymentPhases.INSTALL_SERVICES.plus(100L);

    @Override
    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        final VirtualFile deploymentRoot = getVirtualFileAttachment(context);
        if(deploymentRoot == null || !deploymentRoot.exists() || deploymentRoot.isFile())
            return;

        final VirtualFile serviceLoaderConfig = deploymentRoot.getChild("META-INF/services").getChild(ServiceDeployment.class.getName());
        if(serviceLoaderConfig.exists())
            context.addDeploymentItem(new ServiceDeploymentItem());
    }
}
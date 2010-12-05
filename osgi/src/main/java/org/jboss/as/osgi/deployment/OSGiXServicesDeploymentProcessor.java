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

package org.jboss.as.osgi.deployment;

import java.io.IOException;

import org.jboss.as.deployment.attachment.VirtualFileAttachment;
import org.jboss.as.deployment.module.OSGiDeploymentAttachment;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.deployment.deployer.DeploymentFactory;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.jboss.osgi.vfs.AbstractVFS;
import org.jboss.vfs.VirtualFile;
import org.osgi.framework.Version;

/**
 * Processes deployments that contain META-INF/jbosgi-xservice.properties
 *
 * @author Thomas.Diesler@jboss.com
 * @since 20-Sep-2010
 */
public class OSGiXServicesDeploymentProcessor implements DeploymentUnitProcessor {

    // private static final Logger log = Logger.getLogger("org.jboss.as.osgi");

    @Override
    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {

        // Check if we already have an OSGi deployment
        Deployment deployment = OSGiDeploymentAttachment.getAttachment(context);
        if (deployment != null)
            return;

        // Get the OSGi XService properties
        String resName = "META-INF/jbosgi-xservice.properties";
        VirtualFile virtualFile = VirtualFileAttachment.getVirtualFileAttachment(context);
        VirtualFile xserviceFile = virtualFile.getChild(resName);
        if (xserviceFile.exists()) {
            try {
                OSGiMetaData metadata = OSGiMetaDataBuilder.load(xserviceFile.openStream());
                String location = virtualFile.getPathName();
                String symbolicName = metadata.getBundleSymbolicName();
                Version version = metadata.getBundleVersion();
                deployment = DeploymentFactory.createDeployment(AbstractVFS.adapt(virtualFile), location, symbolicName, version);
                deployment.addAttachment(OSGiMetaData.class, metadata);
                OSGiMetaDataAttachment.attachOSGiMetaData(context, metadata);
                OSGiDeploymentAttachment.attachDeployment(context, deployment);
            } catch (IOException ex) {
                throw new DeploymentUnitProcessingException("Cannot parse: " + xserviceFile);
            }
        }
    }
}

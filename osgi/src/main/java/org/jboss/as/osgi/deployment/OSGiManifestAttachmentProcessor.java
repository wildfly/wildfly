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

import java.util.jar.Manifest;

import org.jboss.as.deployment.Attachments;
import org.jboss.as.deployment.unit.DeploymentUnit;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.deployment.unit.DeploymentPhaseContext;
import org.jboss.osgi.spi.util.BundleInfo;

/**
 * Processes deployments that contain a valid OSGi manifest.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 02-Dec-2010
 */
public class OSGiManifestAttachmentProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        // Check if we already have an OSGiManifestAttachment
        Manifest manifest = phaseContext.getAttachment(Attachments.OSGI_MANIFEST);
        if (manifest != null)
            return;

        // Check whether this is an OSGi manifest
        manifest = phaseContext.getAttachment(Attachments.MANIFEST);
        if (BundleInfo.isValidateBundleManifest(manifest)) {
            phaseContext.putAttachment(Attachments.OSGI_MANIFEST, manifest);
        }
    }

    public void undeploy(DeploymentUnit context) {
    }
}

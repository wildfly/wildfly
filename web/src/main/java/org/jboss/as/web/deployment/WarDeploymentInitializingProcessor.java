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

package org.jboss.as.web.deployment;

import java.util.Locale;
import java.util.jar.Manifest;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.ManifestHelper;

/**
 * Processor that marks a war deployment.
 *
 * @author John Bailey
 * @author Thomas.Diesler@jboss.com
 */
public class WarDeploymentInitializingProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        String deploymentName = deploymentUnit.getName().toLowerCase(Locale.ENGLISH);
        if (deploymentName.endsWith(".war") || deploymentName.endsWith(".wab")) {
            DeploymentTypeMarker.setType(DeploymentType.WAR, deploymentUnit);
            return;
        }

        if (isWebApplicationBundle(deploymentUnit)) {
            DeploymentTypeMarker.setType(DeploymentType.WAR, deploymentUnit);
            return;
        }
    }

    public static boolean isWebApplicationBundle(DeploymentUnit depUnit) {

        // JAR deployments may contain OSGi metadata with a "Web-ContextPath" header
        // This qualifies them as OSGi Web Application Bundle (WAB)
        String deploymentName = depUnit.getName().toLowerCase(Locale.ENGLISH);
        Manifest manifest = depUnit.getAttachment(Attachments.OSGI_MANIFEST);
        if (manifest != null && deploymentName.endsWith(".jar")) {
            if (ManifestHelper.hasMainAttributeValue(manifest, "Web-ContextPath")) {
                return true;
            }
        }

        // The webbundle scheme can also be used for plain war deployemnts that are then
        // transformed into an OSGi Web Application Bundle (WAB)
        if (deploymentName.startsWith("webbundle://")) {
            return true;
        }

        return false;
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
    }
}

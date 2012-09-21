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

import static org.jboss.as.osgi.deployment.BundleDeploymentProcessor.getAnnotation;

import org.jboss.as.controller.client.DeploymentMetadata;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.osgi.deployment.deployer.Deployment;

/**
 * Sset the start level for a bundle deployment
 *
 * @author Thomas.Diesler@jboss.com
 * @since 03-Aug-2012
 */
public class BundleStartLevelProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        Deployment deployment = depUnit.getAttachment(OSGiConstants.DEPLOYMENT_KEY);
        if (deployment == null)
            return;

        Integer level = getDeploymentStartLevel(depUnit);
        if (level != null) {
            deployment.setStartLevel(level);
            int frameworkLevel = depUnit.getAttachment(OSGiConstants.START_LEVEL_KEY).getStartLevel();
            if (level > frameworkLevel) {
                deployment.setAutoStart(false);
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private DeploymentMetadata getDeploymentMetadata(final DeploymentUnit depUnit) {
        DeploymentMetadata metadata = depUnit.getAttachment(Attachments.DEPLOYMENT_METADATA);
        if (metadata == null && depUnit.getParent() != null) {
            metadata = depUnit.getParent().getAttachment(Attachments.DEPLOYMENT_METADATA);
        }
        return metadata;
    }

    private Integer getDeploymentStartLevel(DeploymentUnit depUnit) {
        DeploymentMetadata metadata = getDeploymentMetadata(depUnit);
        Integer level = (Integer) metadata.getValue(ClientConstants.DEPLOYMENT_METADATA_BUNDLE_STARTLEVEL);
        if (level == null) {
            AnnotationInstance slAware = getAnnotation(depUnit, "org.jboss.arquillian.osgi.StartLevelAware");
            if (slAware != null) {
                AnnotationValue value = slAware.value("startLevel");
                level = value != null ? value.asInt() : null;
            }
        }
        if (level == null) {
            AnnotationInstance marker = getAnnotation(depUnit, "org.jboss.as.osgi.DeploymentMarker");
            if (marker != null) {
                AnnotationValue value = marker.value("startLevel");
                level = value != null ? value.asInt() : null;
            }
        }
        return level;
    }
}

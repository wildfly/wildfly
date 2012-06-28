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

import static org.jboss.as.controller.client.helpers.ClientConstants.DEPLOYMENT_METADATA_BUNDLE_STARTLEVEL;
import static org.jboss.as.controller.client.helpers.ClientConstants.DEPLOYMENT_METADATA_START_POLICY;

import java.util.List;
import org.jboss.as.controller.client.DeploymentMetadata;
import org.jboss.as.controller.client.helpers.ClientConstants.StartPolicy;
import org.jboss.as.osgi.DeploymentMarker;
import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.osgi.service.BundleInstallIntegration;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.deployment.deployer.DeploymentFactory;
import org.jboss.osgi.spi.BundleInfo;

/**
 * Processes deployments that have OSGi metadata attached.
 *
 * If so, it creates an {@link Deployment}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 20-Sep-2010
 */
public class BundleDeploymentProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        final String contextName = depUnit.getName();

        // Check if {@link BundleInstallIntegration} provided the {@link Deployment}
        Deployment deployment = BundleInstallIntegration.removeDeployment(contextName);
        if (deployment != null) {
            deployment.setAutoStart(false);
        }

        // Check for attached BundleInfo
        BundleInfo info = depUnit.getAttachment(Attachments.BUNDLE_INFO_KEY);
        if (deployment == null && info != null) {
            deployment = DeploymentFactory.createDeployment(info);
            deployment.addAttachment(BundleInfo.class, info);

            // Initialize autostart from the {@link StartPolicy}
            deployment.setAutoStart(getStartPolicy(depUnit) == StartPolicy.AUTO);

            // Prevent autostart for marked deployments
            CompositeIndex compositeIndex = depUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
            DotName markerName = DotName.createSimple(DeploymentMarker.class.getName());
            for (AnnotationInstance marker : compositeIndex.getAnnotations(markerName)) {
                if (marker.value("autoStart").asBoolean() == false) {
                    deployment.setAutoStart(false);
                    break;
                }
            }

            // Optionally set the start level specified by the client of the deployment API
            DeploymentMetadata metadata = depUnit.getAttachment(Attachments.DEPLOYMENT_METADATA);
            Integer startLevel = (Integer) metadata.getValue(DEPLOYMENT_METADATA_BUNDLE_STARTLEVEL);
            if (startLevel != null) {
                deployment.setStartLevel(startLevel);
            }

            // Prevent autostart of ARQ deployments
            if (deployment.isAutoStart()) {
                DotName runWithName = DotName.createSimple("org.junit.runner.RunWith");
                List<AnnotationInstance> runWithList = compositeIndex.getAnnotations(runWithName);
                deployment.setAutoStart(runWithList.isEmpty());
            }
        }

        // Attach the deployment
        if (deployment != null) {
            depUnit.putAttachment(OSGiConstants.DEPLOYMENT_KEY, deployment);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
        depUnit.removeAttachment(OSGiConstants.DEPLOYMENT_KEY);
    }

    private StartPolicy getStartPolicy(final DeploymentUnit depUnit) {
        DeploymentMetadata metadata = depUnit.getAttachment(Attachments.DEPLOYMENT_METADATA);
        return StartPolicy.parse((String) metadata.getUserdata().get(DEPLOYMENT_METADATA_START_POLICY));
    }
}

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

import java.util.List;

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
import org.jboss.modules.ModuleIdentifier;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.deployment.deployer.DeploymentFactory;
import org.jboss.osgi.metadata.OSGiMetaData;
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
        BundleInfo info = depUnit.getAttachment(OSGiConstants.BUNDLE_INFO_KEY);
        if (deployment == null && info != null) {
            deployment = DeploymentFactory.createDeployment(info);
            deployment.addAttachment(BundleInfo.class, info);
            OSGiMetaData metadata = depUnit.getAttachment(OSGiConstants.OSGI_METADATA_KEY);
            deployment.setAutoStart(metadata.getFragmentHost() == null);

            // Set the start level and prevent autostart if greater than the Framw
            AnnotationInstance slAware = getAnnotation(depUnit, "org.jboss.arquillian.osgi.StartLevelAware");
            if (slAware != null) {
                int startLevel = slAware.value("startLevel").asInt();
                deployment.setStartLevel(startLevel);
                deployment.setAutoStart(false);
            }

            // Prevent autostart of ARQ deployments
            DotName runWithName = DotName.createSimple("org.junit.runner.RunWith");
            CompositeIndex compositeIndex = depUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
            List<AnnotationInstance> runWithList = compositeIndex.getAnnotations(runWithName);
            deployment.setAutoStart(runWithList.isEmpty());
        }

        if (deployment != null) {
            // Make sure the framework uses the same module id as the server
            ModuleIdentifier identifier = depUnit.getAttachment(Attachments.MODULE_IDENTIFIER);
            deployment.addAttachment(ModuleIdentifier.class, identifier);

            // Attach the deployment
            depUnit.putAttachment(OSGiConstants.DEPLOYMENT_KEY, deployment);
        }
    }

    private AnnotationInstance getAnnotation(DeploymentUnit depUnit, String className) {
        CompositeIndex index = depUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        List<AnnotationInstance> annotations = index.getAnnotations(DotName.createSimple(className));
        return annotations.size() == 1 ? annotations.get(0) : null;
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
        depUnit.removeAttachment(OSGiConstants.DEPLOYMENT_KEY);
    }
}

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

import static org.jboss.as.controller.client.helpers.ClientConstants.DEPLOYMENT_METADATA_START_POLICY;

import java.util.List;

import javax.annotation.ManagedBean;

import org.jboss.as.controller.client.DeploymentMetadata;
import org.jboss.as.controller.client.helpers.ClientConstants.StartPolicy;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.osgi.DeploymentMarker;
import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.osgi.service.BundleInstallIntegration;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.EjbDeploymentMarker;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
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
            StartPolicy startPolicy = getStartPolicy(depUnit);
            OSGiMetaData metadata = info.getOSGiMetadata();
            deployment.setAutoStart(startPolicy == StartPolicy.AUTO && !metadata.isFragment());

            // Prevent autostart for marked deployments
            AnnotationInstance marker = getAnnotation(depUnit, DeploymentMarker.class.getName());
            if (deployment.isAutoStart() && marker != null) {
                AnnotationValue value = marker.value("autoStart");
                deployment.setAutoStart(Boolean.parseBoolean(value.asString()));
            }
        }

        // Attach the deployment
        if (deployment != null) {

            // Make sure the framework uses the same module id as the server
            ModuleIdentifier identifier = depUnit.getAttachment(Attachments.MODULE_IDENTIFIER);
            deployment.addAttachment(ModuleIdentifier.class, identifier);

            // Allow additional dependencies for the set of supported deployemnt types
            if (allowAdditionalModuleDependencies(depUnit)) {
                ModuleSpecification moduleSpec = depUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
                deployment.addAttachment(ModuleSpecification.class, moduleSpec);
            } else {
                // Make this module private so that other modules in the deployment don't create a direct dependency
                ModuleSpecification moduleSpec = depUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
                moduleSpec.setPrivateModule(true);
            }

            // Attach the bundle deployment
            depUnit.putAttachment(OSGiConstants.DEPLOYMENT_KEY, deployment);
        }
    }

    static AnnotationInstance getAnnotation(DeploymentUnit depUnit, String className) {
        CompositeIndex index = depUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        List<AnnotationInstance> annotations = index.getAnnotations(DotName.createSimple(className));
        return annotations.size() == 1 ? annotations.get(0) : null;
    }

    private boolean allowAdditionalModuleDependencies(DeploymentUnit depUnit) {
        boolean isWar = DeploymentTypeMarker.isType(DeploymentType.WAR, depUnit);
        boolean isEjb = EjbDeploymentMarker.isEjbDeployment(depUnit);
        boolean isCDI = getAnnotation(depUnit, ManagedBean.class.getName()) != null;
        return isWar || isEjb || isCDI;
    }

    @Override
    public void undeploy(DeploymentUnit depUnit) {
        depUnit.removeAttachment(OSGiConstants.DEPLOYMENT_KEY);
    }

    private DeploymentMetadata getDeploymentMetadata(final DeploymentUnit depUnit) {
        DeploymentMetadata metadata = depUnit.getAttachment(Attachments.DEPLOYMENT_METADATA);
        if (metadata == null && depUnit.getParent() != null) {
            metadata = depUnit.getParent().getAttachment(Attachments.DEPLOYMENT_METADATA);
        }
        return metadata;
    }

    private StartPolicy getStartPolicy(DeploymentUnit depUnit) {
        DeploymentMetadata metadata = getDeploymentMetadata(depUnit);
        return StartPolicy.parse((String) metadata.getUserdata().get(DEPLOYMENT_METADATA_START_POLICY));
    }
}

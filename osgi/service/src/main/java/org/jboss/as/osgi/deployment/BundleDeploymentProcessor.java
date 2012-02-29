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

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceRegistry;
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

        // Check if we already have an OSGi deployment
        Deployment deployment = OSGiDeploymentAttachment.getDeployment(depUnit);
        if (deployment != null)
            return;

        // Check if {@link InstallHandlerIntegration} provided the {@link Deployment}
        if (deployment == null) {
            ServiceRegistry serviceRegistry = phaseContext.getServiceRegistry();
            ServiceController<Deployment> controller = DeploymentHolderService.getDeployment(serviceRegistry, contextName);
            if (controller != null) {
                deployment = controller.getValue();
                deployment.setAutoStart(false);
                controller.setMode(Mode.REMOVE);
            }
        }

        // Check for attached BundleInfo
        BundleInfo info = BundleInfoAttachment.getBundleInfo(depUnit);
        if (deployment == null && info != null) {
            deployment = DeploymentFactory.createDeployment(info);
            deployment.addAttachment(BundleInfo.class, info);
            deployment.setAutoStart(true);
        }

        // Create the {@link BundleInstallService}
        if (deployment != null) {

            // Process annotations to modify the generated {@link Deployment}
            final DotName runWithName = DotName.createSimple("org.junit.runner.RunWith");
            final CompositeIndex compositeIndex = depUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
            final List<AnnotationInstance> runWithList = compositeIndex.getAnnotations(runWithName);
            if (runWithList.isEmpty() == false) {
                deployment.setAutoStart(false);
            }

            OSGiDeploymentAttachment.attachDeployment(depUnit, deployment);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
        OSGiDeploymentAttachment.detachDeployment(depUnit);
    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.component;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class EEModuleInitialProcessor implements DeploymentUnitProcessor {

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentUnit parent = deploymentUnit.getParent();
        String deploymentUnitName = deploymentUnit.getName();
        final String moduleName;
        if (deploymentUnitName.endsWith(".war") || deploymentUnitName.endsWith(".jar") || deploymentUnitName.endsWith(".ear") || deploymentUnitName.endsWith(".rar")) {
            moduleName = deploymentUnitName.substring(0, deploymentUnitName.length() - 4);
        } else {
            moduleName = deploymentUnitName;
        }
        final String appName;
        if (parent == null) {
            appName = moduleName;
        } else {
            final String parentName = parent.getName();
            if (parentName.endsWith(".ear")) {
                appName = parentName.substring(0, parentName.length() - 4);
            } else {
                appName = parentName;
            }
        }
        deploymentUnit.putAttachment(Attachments.EE_MODULE_DESCRIPTION, new EEModuleDescription(appName, moduleName));
        final EEApplicationClasses classes;
        if(parent == null) {
            classes = new EEApplicationClasses();
        } else {
            EEApplicationClasses parentClasses = parent.getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);
            classes = new EEApplicationClasses(parentClasses);
        }
        deploymentUnit.putAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION, classes);
    }

    public void undeploy(final DeploymentUnit context) {
    }
}

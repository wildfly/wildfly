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

package org.jboss.as.ee.component.deployers;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class EEModuleInitialProcessor implements DeploymentUnitProcessor {

    private final boolean appClient;

    public EEModuleInitialProcessor(boolean appClient) {
        this.appClient = appClient;
    }

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final String deploymentUnitName = deploymentUnit.getName();
        final String moduleName;
        if (deploymentUnitName.endsWith(".war") || deploymentUnitName.endsWith(".jar") || deploymentUnitName.endsWith(".ear") || deploymentUnitName.endsWith(".rar")) {
            moduleName = deploymentUnitName.substring(0, deploymentUnitName.length() - 4);
        } else {
            moduleName = deploymentUnitName;
        }
        final String appName;
        final String earApplicationName = deploymentUnit.getAttachment(Attachments.EAR_APPLICATION_NAME);
        //the ear application name takes into account the name set in application.xml
        //if this is non-null this is always the one we want to use
        if(earApplicationName != null) {
            appName = earApplicationName;
        } else {
            //an appname of null means use the module name
            appName = null;
        }
        deploymentUnit.putAttachment(Attachments.EE_MODULE_DESCRIPTION, new EEModuleDescription(appName, moduleName, earApplicationName, appClient));
    }

    public void undeploy(final DeploymentUnit context) {
    }
}

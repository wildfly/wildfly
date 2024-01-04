/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component.deployers;

import java.util.HashMap;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
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
        deploymentUnit.putAttachment(org.jboss.as.server.deployment.Attachments.COMPONENT_JNDI_DEPENDENCIES, new HashMap<>());
    }
}

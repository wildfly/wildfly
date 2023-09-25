/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.lra.participant.deployment;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.vfs.VirtualFile;
import org.wildfly.extension.microprofile.lra.participant._private.MicroProfileLRAParticipantLogger;

import java.io.IOException;

public class LRAParticipantDeploymentSetupProcessor implements DeploymentUnitProcessor {
    // CDI markers do declare deployment being a CDI project
    private static final String WEB_INF_BEANS_XML = "WEB-INF/beans.xml";
    private static final String META_INF_BEANS_XML = "META-INF/beans.xml";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (LRAAnnotationsUtil.isNotLRADeployment(deploymentUnit)) {
            return;
        }

        addBeanXml(deploymentUnit);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private void addBeanXml(DeploymentUnit deploymentUnit) {
        VirtualFile beanXmlVFile;
        if (DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            beanXmlVFile = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot().getChild(WEB_INF_BEANS_XML);
        } else {
            beanXmlVFile = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot().getChild(META_INF_BEANS_XML);
        }

        if (!beanXmlVFile.exists()) {
            try {
                boolean isCreated = beanXmlVFile.getPhysicalFile().createNewFile();
                MicroProfileLRAParticipantLogger.LOGGER.debugf("The CDI marker file '%s' %s created",
                    beanXmlVFile.getPhysicalFile(), (isCreated ? "was" : "was NOT"));
            } catch (IOException ioe) {
                MicroProfileLRAParticipantLogger.LOGGER.cannotCreateCDIMarkerFile(ioe);
            }
        }
    }
}
/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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
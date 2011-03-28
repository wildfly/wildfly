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

package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.EjbJarDescription;
import org.jboss.as.ejb3.component.description.EjbJarDescriptionMergingUtil;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponentDescription;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.deployment.EjbDeploymentMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * Merges the annotated and the ejb-jar.xml deployment descriptor descriptions for EJBs.
 *
 * @author Jaikiran Pai
 */
public class MergingEjbJarDescriptionProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        // Only process EJB deployments
        if (!EjbDeploymentMarker.isEjbDeployment(deploymentUnit)) {
            return;
        }

        EjbJarDescription ddBasedEjbJarDescription = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.DD_EJB_JAR_DESCRIPTION);
        EjbJarDescription annotationBasedEjbJarDescription = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.ANNOTATION_EJB_JAR_DESCRIPTION);

        if (ddBasedEjbJarDescription == null && annotationBasedEjbJarDescription == null) {
            return;
        }
        EjbJarDescription mergedEjbJarDescription = null;
        if (ddBasedEjbJarDescription == null) {
            mergedEjbJarDescription = annotationBasedEjbJarDescription;
        } else if (annotationBasedEjbJarDescription == null) {
            mergedEjbJarDescription = ddBasedEjbJarDescription;
        } else {
            mergedEjbJarDescription = new EjbJarDescription();
            EjbJarDescriptionMergingUtil.merge(mergedEjbJarDescription, annotationBasedEjbJarDescription, ddBasedEjbJarDescription);
        }
        deploymentUnit.putAttachment(EjbDeploymentAttachmentKeys.MERGED_EJB_JAR_DESCRIPTION, mergedEjbJarDescription);

        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        for (SessionBeanComponentDescription sessionBean : mergedEjbJarDescription.getSessionBeans()) {
            moduleDescription.addComponent(sessionBean);
        }
        for (MessageDrivenComponentDescription mdb : mergedEjbJarDescription.getMessageDrivenBeans()) {
            moduleDescription.addComponent(mdb);
        }


    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}

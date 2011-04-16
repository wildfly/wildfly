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

package org.jboss.as.ejb3.deployment.processors.dd;

import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponentDescription;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.metadata.ejb.spec.MessageDrivenBeanMetaData;

/**
 * User: jpai
 */
public class MessageDrivenBeanXmlDescriptorProcessor extends AbstractEjbXmlDescriptorProcessor<MessageDrivenBeanMetaData> {
    @Override
    protected Class<MessageDrivenBeanMetaData> getMetaDataType() {
        return MessageDrivenBeanMetaData.class;
    }

    @Override
    protected void processBeanMetaData(MessageDrivenBeanMetaData mdb, DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EjbJarDescription ejbModuleDescription = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_DESCRIPTION);
        // get the module description
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        final String applicationName = moduleDescription.getApplicationName();

        String ejbName = mdb.getEjbName();
        String ejbClassName = mdb.getEjbClass();
        MessageDrivenComponentDescription mdbDescription = new MessageDrivenComponentDescription(ejbName, ejbClassName, ejbModuleDescription, deploymentUnit.getServiceName());

        mdbDescription.setMessageListenerInterfaceName(mdb.getMessagingType());

        // Add this component description to the module description
        ejbModuleDescription.getEEModuleDescription().addComponent(mdbDescription);

    }
}

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
package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.EjbJarDescription;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.deployment.EjbDeploymentMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import javax.ejb.MessageDriven;
import java.util.List;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class MessageDrivenAnnotationProcessor implements DeploymentUnitProcessor {
    static final DotName MESSAGE_DRIVEN_ANNOTATION_NAME = DotName.createSimple(MessageDriven.class.getName());

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        // attach the EjbJarDescription based off annotations
        this.attachEjbJarDescriptionIfAbsent(deploymentUnit);

        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        final String moduleName = moduleDescription.getModuleName();
        final String applicationName = moduleDescription.getAppName();
        final CompositeIndex compositeIndex = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (compositeIndex == null) {
            return;
        }
        final List<AnnotationInstance> instances = compositeIndex.getAnnotations(MESSAGE_DRIVEN_ANNOTATION_NAME);
        if (instances == null || instances.isEmpty()) {
            return;
        }
        EjbDeploymentMarker.mark(deploymentUnit);
        for (final AnnotationInstance instance : instances) {
            final AnnotationTarget target = instance.target();
            if (!(target instanceof ClassInfo)) {
                throw new DeploymentUnitProcessingException("The @MessageDriven annotation is only allowed at the class level: " + target);
            }
            final ClassInfo beanClassInfo = (ClassInfo) target;
            final String beanClassName = beanClassInfo.name().toString();
            final String ejbName = beanClassInfo.name().local();
            final AnnotationValue nameValue = instance.value("name");
            final String beanName = nameValue == null || nameValue.asString().isEmpty() ? ejbName : nameValue.asString();

            final String messageListenerInterfaceName = instance.value("messageListenerInterface").asClass().name().toString();
            // TODO: if messageListenerInterface is not set use the implemented interface

            MessageDrivenComponentDescription messageDrivenComponentDescription = new MessageDrivenComponentDescription(beanName, beanClassName, moduleName, applicationName);
            messageDrivenComponentDescription.setMessageListenerInterfaceName(messageListenerInterfaceName);

            // add the mdb description to the EjbJarDescription
            deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.ANNOTATION_EJB_JAR_DESCRIPTION).addMessageDrivenBean(messageDrivenComponentDescription);
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // do nothing
    }

    /**
     * Attaches a new {@link org.jboss.as.ejb3.EjbJarDescription} at {@link org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys#ANNOTATION_EJB_JAR_DESCRIPTION}, to the
     * deployment unit, if the attachment is absent
     *
     * @param deploymentUnit
     */
    private void attachEjbJarDescriptionIfAbsent(DeploymentUnit deploymentUnit) {
        EjbJarDescription ejbJarDescription = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.ANNOTATION_EJB_JAR_DESCRIPTION);
        if (ejbJarDescription == null) {
            deploymentUnit.putAttachment(EjbDeploymentAttachmentKeys.ANNOTATION_EJB_JAR_DESCRIPTION, new EjbJarDescription());
        }
    }

}

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

import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponentDescription;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
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
import org.jboss.logging.Logger;

import javax.ejb.MessageDriven;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class MessageDrivenAnnotationProcessor implements DeploymentUnitProcessor {
    static final DotName MESSAGE_DRIVEN_ANNOTATION_NAME = DotName.createSimple(MessageDriven.class.getName());

    private static final Logger logger = Logger.getLogger(MessageDrivenAnnotationProcessor.class);

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        final CompositeIndex compositeIndex = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        final EEApplicationClasses applicationClassesDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);
        if (compositeIndex == null) {
            return;
        }
        final List<AnnotationInstance> instances = compositeIndex.getAnnotations(MESSAGE_DRIVEN_ANNOTATION_NAME);
        if (instances == null || instances.isEmpty()) {
            return;
        }
        EjbDeploymentMarker.mark(deploymentUnit);
        EjbJarDescription ejbJarDescription = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_DESCRIPTION);
        if (ejbJarDescription == null) {
            ejbJarDescription = new EjbJarDescription(moduleDescription, applicationClassesDescription, deploymentUnit.getName().endsWith(".war"));
            deploymentUnit.putAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_DESCRIPTION, ejbJarDescription);
        }
        for (final AnnotationInstance instance : instances) {
            final AnnotationTarget target = instance.target();
            if (!(target instanceof ClassInfo)) {
                throw new DeploymentUnitProcessingException("The @MessageDriven annotation is only allowed at the class level: " + target);
            }
            final ClassInfo beanClassInfo = (ClassInfo) target;
            // skip if not a valid MDB class
            if (!assertMessageDrivenBeanClassValidity(beanClassInfo)) {
                continue;
            }
            final String beanClassName = beanClassInfo.name().toString();
            final String ejbName = beanClassInfo.name().local();
            final AnnotationValue nameValue = instance.value("name");
            final String beanName = nameValue == null || nameValue.asString().isEmpty() ? ejbName : nameValue.asString();

            final String messageListenerInterfaceName = instance.value("messageListenerInterface").asClass().name().toString();
            // TODO: if messageListenerInterface is not set use the implemented interface

            MessageDrivenComponentDescription messageDrivenComponentDescription = new MessageDrivenComponentDescription(beanName, beanClassName, ejbJarDescription, deploymentUnit.getServiceName());

            messageDrivenComponentDescription.setMessageListenerInterfaceName(messageListenerInterfaceName);

            // add the mdb description to the module description
            if (moduleDescription.getComponentByName(messageDrivenComponentDescription.getComponentName()) == null) {
                moduleDescription.addComponent(messageDrivenComponentDescription);
            }

        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // do nothing
    }

    /**
     * Returns true if the passed <code>messageDrivenBeanClass</code> meets the requirements set by the EJB3 spec about
     * bean implementation classes. The passed <code>messageDrivenBeanClass</code> must not be an interface and must be public
     * and not final and not abstract. If it passes these requirements then this method returns true. Else it returns false.
     *
     * @param messageDrivenBeanClass The session bean class
     * @return
     */
    private static boolean assertMessageDrivenBeanClassValidity(final ClassInfo messageDrivenBeanClass) {
        final short flags = messageDrivenBeanClass.flags();
        final String className = messageDrivenBeanClass.name().toString();
        // must *not* be a interface
        if (Modifier.isInterface(flags)) {
            logger.warn("[EJB3.1 spec, section 5.6.2] Message driven bean implementation class MUST NOT be a interface - "
                    + className + " is an interface, hence won't be considered as a message driven bean");
            return false;
        }
        // bean class must be public, must *not* be abstract or final
        if (!Modifier.isPublic(flags) || Modifier.isAbstract(flags) || Modifier.isFinal(flags)) {
            logger.warn("[EJB3.1 spec, section 5.6.2] Message driven bean implementation class MUST be public, not abstract and not final - "
                    + className + " won't be considered as a message driven bean, since it doesn't meet that requirement");
            return false;
        }
        // valid class
        return true;
    }



}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.component.singleton.SingletonComponentDescription;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.component.stateless.StatelessComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.deployment.EjbDeploymentMarker;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
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
import org.jboss.msc.service.ServiceName;

import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import java.util.List;

/**
 * @author Jaikiran Pai
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class EjbAnnotationProcessor implements DeploymentUnitProcessor {

    private static final Logger logger = Logger.getLogger(EjbAnnotationProcessor.class);
    private static final DotName STATELESS_ANNOTATION = DotName.createSimple(Stateless.class.getName());
    private static final DotName STATEFUL_ANNOTATION = DotName.createSimple(Stateful.class.getName());
    private static final DotName SINGLETON_ANNOTATION = DotName.createSimple(Singleton.class.getName());

    private static enum SessionBeanType {
        STATELESS,
        STATEFUL,
        SINGLETON
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        // get hold of the deployment unit
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }

        // get the module description
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        final String applicationName = moduleDescription.getApplicationName();
        final CompositeIndex compositeIndex = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (compositeIndex == null) {
            if (logger.isTraceEnabled()) {
                logger.trace("Skipping EJB annotation processing since no composite annotation index found in unit: " + deploymentUnit);
            }
            return;
        }

        // Find and process any @Stateless bean annotations
        final List<AnnotationInstance> slsbAnnotations = compositeIndex.getAnnotations(STATELESS_ANNOTATION);
        if (!slsbAnnotations.isEmpty()) {
            processSessionBeans(deploymentUnit, slsbAnnotations, SessionBeanType.STATELESS);
            // mark this as an EJB deployment
            EjbDeploymentMarker.mark(deploymentUnit);
        }

        // Find and process any @Stateful bean annotations
        final List<AnnotationInstance> sfsbAnnotations = compositeIndex.getAnnotations(STATEFUL_ANNOTATION);
        if (!sfsbAnnotations.isEmpty()) {
            processSessionBeans(deploymentUnit, sfsbAnnotations, SessionBeanType.STATEFUL);
            // mark this as an EJB deployment
            EjbDeploymentMarker.mark(deploymentUnit);
        }

        // Find and process any @Singleton bean annotations
        final List<AnnotationInstance> sbAnnotations = compositeIndex.getAnnotations(SINGLETON_ANNOTATION);
        if (!sbAnnotations.isEmpty()) {
            processSessionBeans(deploymentUnit, sbAnnotations, SessionBeanType.SINGLETON);
            // mark this as an EJB deployment
            EjbDeploymentMarker.mark(deploymentUnit);
        }
    }

    private void processSessionBeans(final DeploymentUnit deploymentUnit, final List<AnnotationInstance> sessionBeanAnnotations, final SessionBeanType sessionBeanType) {
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);

        EjbJarDescription ejbJarDescription = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_DESCRIPTION);
        if (ejbJarDescription == null) {
            ejbJarDescription = new EjbJarDescription(moduleDescription, deploymentUnit.getName().endsWith(".war"));
            deploymentUnit.putAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_DESCRIPTION, ejbJarDescription);
        }

        final String applicationName = moduleDescription.getApplicationName();
        final ServiceName deploymentUnitServiceName = deploymentUnit.getServiceName();
        // process these session bean annotations and create component descriptions out of it
        for (final AnnotationInstance sessionBeanAnnotation : sessionBeanAnnotations) {
            final AnnotationTarget target = sessionBeanAnnotation.target();
            if (!(target instanceof ClassInfo)) {
                // Let's just WARN and move on. No need to throw an error
                logger.warn(sessionBeanAnnotation.name() + " annotation is expected to be only on classes. " + target + " is not a class");
                continue;
            }
            final ClassInfo sessionBeanClassInfo = (ClassInfo) target;
            final String beanClassName = sessionBeanClassInfo.name().toString();
            final String ejbName = sessionBeanClassInfo.name().local();
            final AnnotationValue nameValue = sessionBeanAnnotation.value("name");
            final String beanName = nameValue == null || nameValue.asString().isEmpty() ? ejbName : nameValue.asString();

            SessionBeanComponentDescription sessionBeanDescription = null;
            switch (sessionBeanType) {
                case STATELESS:
                    sessionBeanDescription = new StatelessComponentDescription(beanName, beanClassName, ejbJarDescription, deploymentUnitServiceName);
                    break;
                case STATEFUL:
                    sessionBeanDescription = new StatefulComponentDescription(beanName, beanClassName, ejbJarDescription, deploymentUnitServiceName);
                    break;
                case SINGLETON:
                    sessionBeanDescription = new SingletonComponentDescription(beanName, beanClassName, ejbJarDescription, deploymentUnitServiceName);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown session bean type: " + sessionBeanType);
            }

            // Add this component description to module description
            if (ejbJarDescription.getEEModuleDescription().getComponentByName(sessionBeanDescription.getComponentName()) == null) {
                ejbJarDescription.getEEModuleDescription().addComponent(sessionBeanDescription);
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

}

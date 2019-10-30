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

package org.jboss.as.ejb3.deployment.processors.merging;

import java.util.Set;

import javax.ejb.DependsOn;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEApplicationDescription;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.metadata.ClassAnnotationInformation;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.singleton.SingletonComponentDescription;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.metadata.ejb.spec.SessionBean31MetaData;

import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;

/**
 * @author Stuart Douglas
 * @author James R. Perkins Jr. (jrp)
 */
public class EjbDependsOnMergingProcessor extends AbstractMergingProcessor<EJBComponentDescription> {

    public EjbDependsOnMergingProcessor() {
        super(EJBComponentDescription.class);
    }

    @Override
    protected void handleAnnotations(final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final EJBComponentDescription description) throws DeploymentUnitProcessingException {
        final EEModuleClassDescription classDescription = applicationClasses.getClassByName(componentClass.getName());
        if (classDescription == null) {
            return;
        }

        final EEApplicationDescription applicationDescription = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_DESCRIPTION);
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT);

        //we ony care about annotations on the actual class
        final ClassAnnotationInformation<DependsOn, String[]> dependsOnClassAnnotationInformation = classDescription.getAnnotationInformation(DependsOn.class);
        if (dependsOnClassAnnotationInformation != null) {
            if (!dependsOnClassAnnotationInformation.getClassLevelAnnotations().isEmpty()) {
                final String[] annotationValues = dependsOnClassAnnotationInformation.getClassLevelAnnotations().get(0);
                setupDependencies(description, applicationDescription, deploymentRoot, annotationValues);
            }
        }
    }

    @Override
    protected void handleDeploymentDescriptor(final DeploymentUnit deploymentUnit, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final EJBComponentDescription description) throws DeploymentUnitProcessingException {

        final EEApplicationDescription applicationDescription = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_DESCRIPTION);
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT);

        if (description.getDescriptorData() instanceof SessionBean31MetaData) {
            SessionBean31MetaData metaData = (SessionBean31MetaData) description.getDescriptorData();
            if (metaData.getDependsOn() != null) {
                setupDependencies(description, applicationDescription, deploymentRoot, metaData.getDependsOn());
            }
        }

    }


    private void setupDependencies(final EJBComponentDescription description, final EEApplicationDescription applicationDescription, final ResourceRoot deploymentRoot, final String[] annotationValues) throws DeploymentUnitProcessingException {
        for (final String annotationValue : annotationValues) {

            final Set<ComponentDescription> components = applicationDescription.getComponents(annotationValue, deploymentRoot.getRoot());
            if (components.isEmpty()) {
                throw EjbLogger.ROOT_LOGGER.failToFindEjbRefByDependsOn(annotationValue, description.getComponentClassName());
            } else if (components.size() != 1) {
                throw EjbLogger.ROOT_LOGGER.failToCallEjbRefByDependsOn(annotationValue, description.getComponentClassName(), components);
            }
            final ComponentDescription component = components.iterator().next();

            description.addDependency(component.getStartServiceName());
            if (description instanceof SingletonComponentDescription) {
                ((SingletonComponentDescription)description).getDependsOn().add(component.getStartServiceName());
                if (ROOT_LOGGER.isDebugEnabled()) {
                    ROOT_LOGGER.debugf(description.getEJBName() + " bean is dependent on " + component.getComponentName());
                }
            }
        }
    }
}

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

import org.jboss.as.ee.component.AbstractComponentConfigProcessor;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceName;

import javax.ejb.DependsOn;
import javax.ejb.Singleton;
import java.util.List;
import java.util.Map;

/**
 * @author James R. Perkins Jr. (jrp)
 *
 */
public class EjbDependsOnAnnotationProcessor extends AbstractComponentConfigProcessor {
    /**
     * Logger
     */
    private static final Logger logger = Logger.getLogger(EjbDependsOnAnnotationProcessor.class);

    @Override
    protected final void processComponentConfig(final DeploymentUnit deploymentUnit, final DeploymentPhaseContext phaseContext,
            final CompositeIndex index, final ComponentDescription componentDescription)
            throws DeploymentUnitProcessingException {
        final ClassInfo beanClass = index.getClassByName(DotName.createSimple(componentDescription.getComponentClassName()));
        if (beanClass == null) {
            return; // We can't continue without the annotation index info.
        }
        Class<SessionBeanComponentDescription> componentDescriptionType = SessionBeanComponentDescription.class;
        // Only process EJB deployments and components that are applicable
        if (!EjbDeploymentMarker.isEjbDeployment(deploymentUnit) || !(componentDescriptionType.isAssignableFrom(componentDescription.getClass()))) {
            return;
        }
        final Map<DotName, List<AnnotationInstance>> annotationsOnBean = beanClass.annotations();
        if (annotationsOnBean == null || annotationsOnBean.isEmpty()) {
            return;
        }

        final SessionBeanComponentDescription singletonComponentDescription = componentDescriptionType.cast(componentDescription);
        final List<AnnotationInstance> dependsOnAnnotations = annotationsOnBean.get(DotName.createSimple(DependsOn.class
                .getName()));
        if (dependsOnAnnotations == null || dependsOnAnnotations.isEmpty()) {
            return;
        }
        validate(annotationsOnBean, dependsOnAnnotations, singletonComponentDescription.getEJBName());
        final AnnotationInstance dependsOnAnnotation = dependsOnAnnotations.get(0);
        // Add the dependencies
        final List<AnnotationValue> annotationValues = dependsOnAnnotation.values();
        for (AnnotationValue annotationValue : annotationValues) {
            componentDescription.addDependency(createServiceName(deploymentUnit, annotationValue.asString(), null),
                    DependencyType.REQUIRED);
        }
        logger.info(singletonComponentDescription.getEJBName() + " bean has @DependsOn");
    }

    /**
     * Runs some simple validation.
     *
     * @param annotationsOnBean
     * @param dependsOnAnnotations
     * @param beanName
     * @throws DeploymentUnitProcessingException if invalid.
     */
    private void validate(final Map<DotName, List<AnnotationInstance>> annotationsOnBean,final List<AnnotationInstance> dependsOnAnnotations, final String beanName) throws DeploymentUnitProcessingException {
        if (dependsOnAnnotations.size() > 1) {
            throw new DeploymentUnitProcessingException("More than one @DependsOn annotation found on bean: " + beanName);
        }

        final AnnotationInstance dependsOnAnnotation = dependsOnAnnotations.get(0);

        // The @DependsOn must be on a @Singleton
        final List<AnnotationInstance> singletonAnnotations = annotationsOnBean.get(DotName.createSimple(Singleton.class
                .getName()));
        if (singletonAnnotations == null || singletonAnnotations.isEmpty()) {
            throw new DeploymentUnitProcessingException(
                    "@DependsOn can appear only on a class annotated with @Singleton. Target: " + dependsOnAnnotation.target()
                            + " is not a class.");
        }
        if (dependsOnAnnotation.target() instanceof ClassInfo == false) {
            throw new DeploymentUnitProcessingException("@DependsOn can appear only on a class. Target: "
                    + dependsOnAnnotation.target() + " is not a class.");
        }
    }

    // TODO - This should be externalized to share with EjbResourcesInjectionAnnotationProcessor
    private ServiceName createServiceName(final DeploymentUnit deploymentUnit, final String beanName, final String beanInterface) {
        final ServiceName beanServiceName = deploymentUnit.getServiceName().append("component").append(beanName).append("START");
        return beanServiceName;
    }
}

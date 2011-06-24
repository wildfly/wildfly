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
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.DependencyConfigurator;
import org.jboss.as.ee.component.EEApplicationDescription;
import org.jboss.as.ejb3.component.singleton.SingletonComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceName;

import javax.ejb.DependsOn;
import javax.ejb.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author James R. Perkins Jr. (jrp)
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
        final Class<SingletonComponentDescription> componentDescriptionType = SingletonComponentDescription.class;
        // Only process EJB deployments and components that are applicable
        if (!EjbDeploymentMarker.isEjbDeployment(deploymentUnit) || !(componentDescriptionType.isAssignableFrom(componentDescription.getClass()))) {
            return;
        }
        final Map<DotName, List<AnnotationInstance>> annotationsOnBean = beanClass.annotations();
        if (annotationsOnBean == null || annotationsOnBean.isEmpty()) {
            return;
        }

        final EEApplicationDescription applicationDescription = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_DESCRIPTION);
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT);

        final SingletonComponentDescription singletonComponentDescription = componentDescriptionType.cast(componentDescription);
        final List<AnnotationInstance> dependsOnAnnotations = annotationsOnBean.get(DotName.createSimple(DependsOn.class
                .getName()));
        if (dependsOnAnnotations == null || dependsOnAnnotations.isEmpty()) {
            return;
        }
        validate(annotationsOnBean, dependsOnAnnotations, singletonComponentDescription.getEJBName());
        final AnnotationInstance dependsOnAnnotation = dependsOnAnnotations.get(0);
        // Add the dependencies
        final String[] annotationValues = dependsOnAnnotation.value().asStringArray();
        for (String annotationValue : annotationValues) {

            final Set<ComponentDescription> components = applicationDescription.getComponents(annotationValue, deploymentRoot.getRoot());
            if (components.isEmpty()) {
                throw new DeploymentUnitProcessingException("Could not find EJB " + annotationValue + " referenced by @DependsOn annotation in " + componentDescription.getComponentClassName());
            } else if (components.size() != 1) {
                throw new DeploymentUnitProcessingException("More than one EJB called" + annotationValue + " referenced by @DependsOn annotation in " + componentDescription.getComponentClassName() + " Components: " + components);
            }
            final ComponentDescription component = components.iterator().next();

            final ServiceName serviceName = createServiceName(component);
            singletonComponentDescription.getDependsOn().add(serviceName);
            componentDescription.addDependency(createServiceName(component),
                    DependencyType.REQUIRED);
        }
        logger.info(singletonComponentDescription.getEJBName() + " bean has @DependsOn");
        componentDescription.getConfigurators().add(new ComponentConfigurator() {
            @Override
            public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                configuration.getStartDependencies().add(new DependencyConfigurator() {
                    @Override
                    public void configureDependency(final ServiceBuilder<?> serviceBuilder) throws DeploymentUnitProcessingException {
                        for(ServiceName dep : singletonComponentDescription.getDependsOn()) {
                            serviceBuilder.addDependency(dep);
                        }
                    }
                });
            }
        });
    }

    /**
     * Runs some simple validation.
     *
     * @param annotationsOnBean
     * @param dependsOnAnnotations
     * @param beanName
     * @throws DeploymentUnitProcessingException
     *          if invalid.
     */
    private void validate(final Map<DotName, List<AnnotationInstance>> annotationsOnBean, final List<AnnotationInstance> dependsOnAnnotations, final String beanName) throws DeploymentUnitProcessingException {
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
    private ServiceName createServiceName(final ComponentDescription componentDescription) {
        final ServiceName beanServiceName = componentDescription.getServiceName().append("START");
        return beanServiceName;
    }
}

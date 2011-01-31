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

package org.jboss.as.managedbean.processors;

import java.util.List;
import java.util.Map;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.injection.ResourceInjectionConfiguration;
import org.jboss.as.ee.component.service.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

/**
 * Deployment processor responsible for analyzing each attached {@link org.jboss.as.ee.component.ComponentConfiguration} instance and determining if
 * any of its resource injections target a managed bean.  If so it will set the correct lookup target on the resource
 * injection configuration.
 *
 * @author John Bailey
 */
public class ManagedBeanResourceTargetProcessor implements DeploymentUnitProcessor {

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final List<ComponentConfiguration> containerConfigs = deploymentUnit.getAttachment(Attachments.COMPONENT_CONFIGS);
        if (containerConfigs == null || containerConfigs.isEmpty()) {
            return;
        }

        final CompositeIndex index = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (index == null) {
            return;
        }

        for (ComponentConfiguration containerConfig : containerConfigs) {
            final List<ResourceInjectionConfiguration> resourceInjectionConfigurations = containerConfig.getResourceInjectionConfigs();
            for(ResourceInjectionConfiguration resourceConfig : resourceInjectionConfigurations) {
                if(resourceConfig.getTargetContextName() != null) continue;

                final ClassInfo targetClass = index.getClassByName(DotName.createSimple(resourceConfig.getInjectedType()));
                if(targetClass == null) continue;

                final Map<DotName, List<AnnotationInstance>> classAnnotations = targetClass.annotations();
                if(classAnnotations == null) continue;

                final List<AnnotationInstance> managedBeanAnnotationInstances = classAnnotations.get(ManagedBeanAnnotationProcessor.MANAGED_BEAN_ANNOTATION_NAME);
                if(managedBeanAnnotationInstances == null || managedBeanAnnotationInstances.isEmpty()) continue;

                final AnnotationInstance instance = managedBeanAnnotationInstances.get(0); // We only allow one @ManagedBean

                final AnnotationValue declaredNameValue = instance.value();
                final String declaredValue = declaredNameValue != null ? declaredNameValue.asString() : null;
                final String targetName =  declaredValue != null && !declaredValue.isEmpty() ? declaredValue : targetClass.name().toString();
                resourceConfig.setTargetContextName(targetName);
            }
        }
    }

    public void undeploy(DeploymentUnit context) {
    }
}

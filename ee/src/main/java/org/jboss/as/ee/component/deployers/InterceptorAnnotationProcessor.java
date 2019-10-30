/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.component.deployers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.interceptor.ExcludeClassInterceptors;
import javax.interceptor.ExcludeDefaultInterceptors;
import javax.interceptor.Interceptors;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.InterceptorDescription;
import org.jboss.as.ee.metadata.MetadataCompleteMarker;
import org.jboss.as.ee.metadata.MethodAnnotationAggregator;
import org.jboss.as.ee.metadata.RuntimeAnnotationInformation;
import org.jboss.as.ee.utils.ClassLoadingUtils;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.proxy.MethodIdentifier;

/**
 * Processor that takes interceptor information from the class description and applies it to components
 *
 * @author Stuart Douglas
 */
public class InterceptorAnnotationProcessor implements DeploymentUnitProcessor {


    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final Collection<ComponentDescription> componentConfigurations = eeModuleDescription.getComponentDescriptions();
        final DeploymentReflectionIndex deploymentReflectionIndex = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);
        final EEApplicationClasses applicationClasses = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);

        if (MetadataCompleteMarker.isMetadataComplete(deploymentUnit)) {
            return;
        }
        if (componentConfigurations == null || componentConfigurations.isEmpty()) {
            return;
        }

        for (final ComponentDescription description : componentConfigurations) {
            processComponentConfig(applicationClasses, deploymentReflectionIndex, description, deploymentUnit);
        }
    }

    private void processComponentConfig(final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final ComponentDescription description, DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException {

        final Class<?> componentClass;
        try {
            componentClass = ClassLoadingUtils.loadClass(description.getComponentClassName(), deploymentUnit);
        } catch (Throwable e) {
            //just ignore the class for now.
            //if it is an optional component this is ok, if it is not an optional component
            //it will fail at configure time anyway
            return;
        }
        handleAnnotations(applicationClasses, deploymentReflectionIndex, componentClass, description);
    }


    private void handleAnnotations(final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final ComponentDescription description) {

        final List<Class> heirachy = new ArrayList<Class>();
        Class c = componentClass;
        while (c != Object.class && c != null) {
            heirachy.add(c);
            c = c.getSuperclass();
        }
        Collections.reverse(heirachy);


        final RuntimeAnnotationInformation<Boolean> excludeDefaultInterceptors = MethodAnnotationAggregator.runtimeAnnotationInformation(componentClass, applicationClasses, deploymentReflectionIndex, ExcludeDefaultInterceptors.class);
        if (excludeDefaultInterceptors.getClassAnnotations().containsKey(componentClass.getName())) {
            description.setExcludeDefaultInterceptors(true);
        }
        for (final Map.Entry<Method, List<Boolean>> entry : excludeDefaultInterceptors.getMethodAnnotations().entrySet()) {
            description.excludeDefaultInterceptors(MethodIdentifier.getIdentifierForMethod(entry.getKey()));
        }
        final RuntimeAnnotationInformation<Boolean> excludeClassInterceptors = MethodAnnotationAggregator.runtimeAnnotationInformation(componentClass, applicationClasses, deploymentReflectionIndex, ExcludeClassInterceptors.class);
        for (final Map.Entry<Method, List<Boolean>> entry : excludeClassInterceptors.getMethodAnnotations().entrySet()) {
            description.excludeClassInterceptors(MethodIdentifier.getIdentifierForMethod(entry.getKey()));
        }

        final RuntimeAnnotationInformation<String[]> interceptors = MethodAnnotationAggregator.runtimeAnnotationInformation(componentClass, applicationClasses, deploymentReflectionIndex, Interceptors.class);

        //walk the class heirachy in reverse
        for (final Class<?> clazz : heirachy) {
            final List<String[]> classInterceptors = interceptors.getClassAnnotations().get(clazz.getName());
            if (classInterceptors != null) {
                for (final String interceptor : classInterceptors.get(0)) {
                    description.addClassInterceptor(new InterceptorDescription(interceptor));
                }
            }
        }

        for (final Map.Entry<Method, List<String[]>> entry : interceptors.getMethodAnnotations().entrySet()) {
            final MethodIdentifier method = MethodIdentifier.getIdentifierForMethod(entry.getKey());
            for (final String interceptor : entry.getValue().get(0)) {
                description.addMethodInterceptor(method, new InterceptorDescription(interceptor));
            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }

}

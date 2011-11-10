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

package org.jboss.as.ejb3.deployment.processors.merging;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;

import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.metadata.MethodAnnotationAggregator;
import org.jboss.as.ee.metadata.RuntimeAnnotationInformation;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.deployment.processors.dd.MethodResolutionUtils;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.metadata.ejb.spec.SessionBeanMetaData;
import org.jboss.metadata.javaee.spec.LifecycleCallbackMetaData;
import org.jboss.metadata.javaee.spec.LifecycleCallbacksMetaData;

/**
 * @author Paul Ferraro
 *
 */
public class SessionPassivationMergingProcessor extends AbstractMergingProcessor<StatefulComponentDescription> {

    public SessionPassivationMergingProcessor() {
        super(StatefulComponentDescription.class);
    }

    @Override
    protected void handleAnnotations(final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final StatefulComponentDescription description) throws DeploymentUnitProcessingException {

        RuntimeAnnotationInformation<Boolean> prePassivate = MethodAnnotationAggregator.runtimeAnnotationInformation(componentClass, applicationClasses, deploymentReflectionIndex, PrePassivate.class);
        Map<Method, List<Boolean>> annotations = prePassivate.getMethodAnnotations();
        if (!annotations.isEmpty()) {
            for (Method method: annotations.keySet()) {
                description.addPrePassivateMethod(method);
            }
        }

        RuntimeAnnotationInformation<Boolean> postActivate = MethodAnnotationAggregator.runtimeAnnotationInformation(componentClass, applicationClasses, deploymentReflectionIndex, PostActivate.class);
        annotations = postActivate.getMethodAnnotations();
        if (!annotations.isEmpty()) {
            for (Method method: annotations.keySet()) {
                description.addPostActivateMethod(method);
            }
        }
    }

    @Override
    protected void handleDeploymentDescriptor(final DeploymentUnit deploymentUnit, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final StatefulComponentDescription description) throws DeploymentUnitProcessingException {

        final DeploymentReflectionIndex reflectionIndex = deploymentUnit.getAttachment(Attachments.REFLECTION_INDEX);

        SessionBeanMetaData data = description.getDescriptorData();
        if (data != null) {
            LifecycleCallbacksMetaData callbacks = data.getPrePassivates();
            if (callbacks != null) {
                for (LifecycleCallbackMetaData callback: callbacks) {
                    description.addPrePassivateMethod(MethodResolutionUtils.resolveMethod(callback.getMethodName(), null, componentClass, reflectionIndex));
                }
            }
            callbacks = data.getPostActivates();
            if (callbacks != null) {
                for (LifecycleCallbackMetaData callback: callbacks) {
                    description.addPostActivateMethod(MethodResolutionUtils.resolveMethod(callback.getMethodName(), null, componentClass, reflectionIndex));
                }
            }
        }
    }
}

/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.metadata.ClassAnnotationInformation;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.deployment.processors.dd.MethodResolutionUtils;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.metadata.ejb.spec.NamedMethodMetaData;
import org.jboss.metadata.ejb.spec.RemoveMethodMetaData;
import org.jboss.metadata.ejb.spec.SessionBeanMetaData;

import javax.ejb.Remove;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Class that can merge {@link javax.ejb.Remove}
 *
 * @author Stuart Douglas
 */
public class RemoveMethodMergingProcessor extends AbstractMergingProcessor<StatefulComponentDescription> {


    public RemoveMethodMergingProcessor() {
        super(StatefulComponentDescription.class);
    }

    protected void handleAnnotations(final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final StatefulComponentDescription componentConfiguration) throws DeploymentUnitProcessingException {
        EEModuleClassDescription clazz = applicationClasses.getClassByName(componentClass.getName());
        if (clazz != null) {
            ClassAnnotationInformation<Remove, Boolean> annotations = clazz.getAnnotationInformation(Remove.class);
            if (annotations != null) {
                if (annotations.getMethodLevelAnnotations().size() > 1) {
                    throw new DeploymentUnitProcessingException("More than one @Remove method found on class " + componentClass);
                } else if (!annotations.getMethodLevelAnnotations().isEmpty()) {
                    final Map.Entry<MethodIdentifier, List<Boolean>> entry = annotations.getMethodLevelAnnotations().entrySet().iterator().next();
                    final Boolean retainIfException = entry.getValue().get(0);
                    componentConfiguration.addRemoveMethod(entry.getKey(), retainIfException);
                }
            }
        }

    }

    protected void handleDeploymentDescriptor(final DeploymentUnit deploymentUnit, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final StatefulComponentDescription componentConfiguration) throws DeploymentUnitProcessingException {
        SessionBeanMetaData beanMetaData = componentConfiguration.getDescriptorData();
        if (beanMetaData == null) {
            return;
        }
        if (beanMetaData.getRemoveMethods() == null || beanMetaData.getRemoveMethods().isEmpty()) {
            return;
        }

        final DeploymentReflectionIndex reflectionIndex = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);

        for (final RemoveMethodMetaData removeMethod : beanMetaData.getRemoveMethods()) {
            final NamedMethodMetaData methodData = removeMethod.getBeanMethod();
            final Method method = MethodResolutionUtils.resolveMethod(methodData, componentClass, reflectionIndex);
            componentConfiguration.addRemoveMethod(MethodIdentifier.getIdentifierForMethod(method), removeMethod.isRetainIfException());
        }
    }

}

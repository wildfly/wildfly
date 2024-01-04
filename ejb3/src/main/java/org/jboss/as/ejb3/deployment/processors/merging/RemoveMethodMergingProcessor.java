/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors.merging;

import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.metadata.MethodAnnotationAggregator;
import org.jboss.as.ee.metadata.RuntimeAnnotationInformation;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.deployment.processors.dd.MethodResolutionUtils;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.metadata.ejb.spec.NamedMethodMetaData;
import org.jboss.metadata.ejb.spec.RemoveMethodMetaData;
import org.jboss.metadata.ejb.spec.SessionBeanMetaData;

import jakarta.ejb.Remove;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class that can merge {@link jakarta.ejb.Remove}
 *
 * @author Stuart Douglas
 */
public class RemoveMethodMergingProcessor extends AbstractMergingProcessor<StatefulComponentDescription> {


    public RemoveMethodMergingProcessor() {
        super(StatefulComponentDescription.class);
    }

    protected void handleAnnotations(final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final StatefulComponentDescription componentConfiguration) throws DeploymentUnitProcessingException {
        final RuntimeAnnotationInformation<Boolean> removeMethods = MethodAnnotationAggregator.runtimeAnnotationInformation(componentClass, applicationClasses, deploymentReflectionIndex, Remove.class);
        for (Map.Entry<Method, List<Boolean>> entry : removeMethods.getMethodAnnotations().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                final Boolean retainIfException = entry.getValue().get(0);
                final MethodIdentifier removeMethodIdentifier = MethodIdentifier.getIdentifierForMethod(entry.getKey());
                componentConfiguration.addRemoveMethod(removeMethodIdentifier, retainIfException);
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

        final Set<MethodIdentifier> annotationRemoveMethods = new HashSet<MethodIdentifier>();
        for(final StatefulComponentDescription.StatefulRemoveMethod method : componentConfiguration.getRemoveMethods()) {
            annotationRemoveMethods.add(method.getMethodIdentifier());
        }

        //We loop through twice, as the more more general form with no parameters is applied to all methods with that name
        //while the method that specifies the actual parameters override this
        for (final RemoveMethodMetaData removeMethod : beanMetaData.getRemoveMethods()) {
            if(removeMethod.getBeanMethod().getMethodParams() == null) {
                final NamedMethodMetaData methodData = removeMethod.getBeanMethod();
                final Collection<Method> methods = MethodResolutionUtils.resolveMethods(methodData, componentClass, reflectionIndex);
                for(final Method method : methods) {
                    final Boolean retainIfException = removeMethod.getRetainIfException();
                    final MethodIdentifier methodIdentifier = MethodIdentifier.getIdentifierForMethod(method);
                    if(retainIfException == null) {
                        //if this is null we have to allow annotation values of retainIfException to take precidence
                        if(!annotationRemoveMethods.contains(methodIdentifier)) {
                            componentConfiguration.addRemoveMethod(methodIdentifier, false);
                        }
                    } else {
                        componentConfiguration.addRemoveMethod(methodIdentifier, retainIfException);
                    }
                }
            }
        }
        for (final RemoveMethodMetaData removeMethod : beanMetaData.getRemoveMethods()) {
            if(removeMethod.getBeanMethod().getMethodParams() != null) {
                final NamedMethodMetaData methodData = removeMethod.getBeanMethod();
                final Collection<Method> methods = MethodResolutionUtils.resolveMethods(methodData, componentClass, reflectionIndex);
                for(final Method method : methods) {
                    final Boolean retainIfException = removeMethod.getRetainIfException();
                    final MethodIdentifier methodIdentifier = MethodIdentifier.getIdentifierForMethod(method);
                    if(retainIfException == null) {
                        //if this is null we have to allow annotation values of retainIfException to take precidence
                        if(!annotationRemoveMethods.contains(methodIdentifier)) {
                            componentConfiguration.addRemoveMethod(methodIdentifier, false);
                        }
                    } else {
                        componentConfiguration.addRemoveMethod(methodIdentifier, retainIfException);
                    }
                }
            }
        }
    }

}

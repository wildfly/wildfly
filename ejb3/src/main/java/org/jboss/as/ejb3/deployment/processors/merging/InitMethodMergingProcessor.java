/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors.merging;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import jakarta.ejb.Init;

import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.metadata.MethodAnnotationAggregator;
import org.jboss.as.ee.metadata.RuntimeAnnotationInformation;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.deployment.processors.dd.MethodResolutionUtils;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.metadata.ejb.spec.InitMethodMetaData;
import org.jboss.metadata.ejb.spec.InitMethodsMetaData;
import org.jboss.metadata.ejb.spec.SessionBean31MetaData;
import org.jboss.metadata.ejb.spec.SessionBeanMetaData;

/**
 * Merging processor that handles SFSB init methods.
 * <p/>
 * Note that the corresponding home methods are not resolved as this time
 *
 * @author Stuart Douglas
 */
public class InitMethodMergingProcessor extends AbstractMergingProcessor<StatefulComponentDescription> {

    public InitMethodMergingProcessor() {
        super(StatefulComponentDescription.class);
    }

    @Override
    protected void handleAnnotations(final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final StatefulComponentDescription description) throws DeploymentUnitProcessingException {
        RuntimeAnnotationInformation<String> init = MethodAnnotationAggregator.runtimeAnnotationInformation(componentClass, applicationClasses, deploymentReflectionIndex, Init.class);
        for (Map.Entry<Method, List<String>> entry : init.getMethodAnnotations().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                final String value = entry.getValue().get(0);
                if (value != null && !value.isEmpty()) {
                    description.addInitMethod(entry.getKey(), value);
                } else {
                    description.addInitMethod(entry.getKey(), null);
                }
            }
        }
    }

    @Override
    protected void handleDeploymentDescriptor(final DeploymentUnit deploymentUnit, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final StatefulComponentDescription description) throws DeploymentUnitProcessingException {

        //first look for old school ejbCreate methods
        //we are only looking on the bean class, not sure if that is correct or not
        Class<?> clazz = componentClass;
        while (clazz != Object.class && clazz != null) {
            final ClassReflectionIndex index = deploymentReflectionIndex.getClassIndex(clazz);

            for (Method method : (Iterable<Method>)index.getMethods()) {
                // if there is additional metadata specified for this method
                // it will be overridden below
                if (method.getName().startsWith("ejbCreate")
                        && !description.getInitMethods().containsKey(method)) {
                    description.addInitMethod(method, null);
                }
            }
            clazz = clazz.getSuperclass();
        }


        SessionBeanMetaData data = description.getDescriptorData();
        if (data instanceof SessionBean31MetaData) {
            SessionBean31MetaData metaData = (SessionBean31MetaData) data;
            final InitMethodsMetaData inits = metaData.getInitMethods();
            if (inits != null) {
                for (InitMethodMetaData method : inits) {
                    Method beanMethod = MethodResolutionUtils.resolveMethod(method.getBeanMethod(), componentClass, deploymentReflectionIndex);
                    if (method.getCreateMethod() != null) {
                        description.addInitMethod(beanMethod, method.getCreateMethod().getMethodName());
                    } else {
                        description.addInitMethod(beanMethod, null);
                    }
                }
            }

        }
    }
}

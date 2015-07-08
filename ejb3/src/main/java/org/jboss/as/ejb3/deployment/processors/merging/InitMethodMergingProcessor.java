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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import javax.ejb.Init;

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
                if (method.getName().startsWith("ejbCreate")) {
                    //if there is additional metadata specified for this method
                    //it will be overridden below
                    if (!description.getInitMethods().containsKey(method)) {
                        description.addInitMethod(method, null);
                    }
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

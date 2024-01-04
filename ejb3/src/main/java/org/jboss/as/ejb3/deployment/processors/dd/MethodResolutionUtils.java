/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors.dd;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.metadata.ejb.spec.MethodMetaData;
import org.jboss.metadata.ejb.spec.MethodParametersMetaData;
import org.jboss.metadata.ejb.spec.NamedMethodMetaData;

/**
 * @author Stuart Douglas
 */
public class MethodResolutionUtils {


    public static Method resolveMethod(final NamedMethodMetaData methodData, final Class<?> componentClass, final DeploymentReflectionIndex reflectionIndex) throws DeploymentUnitProcessingException {
        return resolveMethod(methodData.getMethodName(), methodData.getMethodParams(), componentClass, reflectionIndex);
    }

    public static Method resolveMethod(final MethodMetaData methodData, final Class<?> componentClass, final DeploymentReflectionIndex reflectionIndex) throws DeploymentUnitProcessingException {
        return resolveMethod(methodData.getMethodName(), methodData.getMethodParams(), componentClass, reflectionIndex);
    }

    public static Collection<Method> resolveMethods(final NamedMethodMetaData methodData, final Class<?> componentClass, final DeploymentReflectionIndex reflectionIndex) throws DeploymentUnitProcessingException {
        return resolveMethods(methodData.getMethodName(), methodData.getMethodParams(), componentClass, reflectionIndex);
    }

    public static Method resolveMethod(final String methodName, final MethodParametersMetaData parameters, final Class<?> componentClass, final DeploymentReflectionIndex reflectionIndex) throws DeploymentUnitProcessingException {
        final Collection<Method> method = resolveMethods(methodName, parameters, componentClass, reflectionIndex);

        if(method.size() >1) {
            throw EjbLogger.ROOT_LOGGER.moreThanOneMethodWithSameNameOnComponent(methodName, componentClass);
        }
        return method.iterator().next();
    }

    public static Collection<Method> resolveMethods(final String methodName, final MethodParametersMetaData parameters, final Class<?> componentClass, final DeploymentReflectionIndex reflectionIndex) throws DeploymentUnitProcessingException {
        Class<?> clazz = componentClass;
        while (clazz != Object.class && clazz != null) {
            final ClassReflectionIndex classIndex = reflectionIndex.getClassIndex(clazz);
            if (parameters == null) {
                final Collection<Method> methods = classIndex.getAllMethods(methodName);
                if(!methods.isEmpty()) {
                    return methods;
                }
            } else {
                final Collection<Method> methods = classIndex.getAllMethods(methodName, parameters.size());
                for (final Method method : methods) {
                    boolean match = true;
                    for (int i = 0; i < method.getParameterCount(); ++i) {
                        if (!method.getParameterTypes()[i].getName().equals(parameters.get(i))) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        return Collections.singleton(method);
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        throw EjbLogger.ROOT_LOGGER.failToFindMethodInEjbJarXml(componentClass != null ? componentClass.getName() : "null", methodName);

    }
}

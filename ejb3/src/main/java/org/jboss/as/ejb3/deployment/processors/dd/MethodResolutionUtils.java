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
                    for (int i = 0; i < method.getParameterTypes().length; ++i) {
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
        throw EjbLogger.ROOT_LOGGER.failToFindMethodInEjbJarXml(componentClass.getName(), methodName);

    }
}

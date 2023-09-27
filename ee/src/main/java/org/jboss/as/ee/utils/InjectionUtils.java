/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.utils;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Locale;

/**
 * Utility class for injection framework.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author Eduardo Martins
 */
public final class InjectionUtils {

    private InjectionUtils() {
        // forbidden instantiation
    }

    public static AccessibleObject getInjectionTarget(final String injectionTargetClassName, final String injectionTargetName, final ClassLoader classLoader, final DeploymentReflectionIndex deploymentReflectionIndex) throws DeploymentUnitProcessingException {
        final Class<?> injectionTargetClass;
        try {
            injectionTargetClass = classLoader.loadClass(injectionTargetClassName);
        } catch (ClassNotFoundException e) {
            throw EeLogger.ROOT_LOGGER.cannotLoad(e, injectionTargetClassName);
        }
        final ClassReflectionIndex index = deploymentReflectionIndex.getClassIndex(injectionTargetClass);
        String methodName = "set" + injectionTargetName.substring(0, 1).toUpperCase(Locale.ENGLISH) + injectionTargetName.substring(1);

        boolean methodFound = false;
        Method method = null;
        Field field = null;
        Class<?> current = injectionTargetClass;
        while (current != Object.class && current != null && !methodFound) {
            final Collection<Method> methods = index.getAllMethods(methodName);
            for (Method m : methods) {
                if (m.getParameterCount() == 1) {
                    if (m.isBridge() || m.isSynthetic()) {
                        continue;
                    }
                    if (methodFound) {
                        throw EeLogger.ROOT_LOGGER.multipleSetterMethodsFound(injectionTargetName, injectionTargetClassName);
                    }
                    methodFound = true;
                    method = m;
                }
            }
            current = current.getSuperclass();
        }
        if (method == null) {
            current = injectionTargetClass;
            while (current != Object.class && current != null && field == null) {
                field = index.getField(injectionTargetName);
                if (field != null) {
                    break;
                }
                current = current.getSuperclass();
            }
        }
        if (field == null && method == null) {
            throw EeLogger.ROOT_LOGGER.cannotResolveInjectionPoint(injectionTargetName, injectionTargetClassName);
        }

        return field != null ? field : method;
    }
}

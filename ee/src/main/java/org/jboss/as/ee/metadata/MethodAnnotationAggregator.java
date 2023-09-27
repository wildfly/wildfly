/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.metadata;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.proxy.MethodIdentifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class which can turn a pre-runtime description of annotations into a runtime description.
 * <p/>
 * This correctly handles overridden methods, so the annotations on overridden methods will not show up in the result
 *
 * @author Stuart Douglas
 */
public class MethodAnnotationAggregator {

    public static <A extends Annotation, T> RuntimeAnnotationInformation<T> runtimeAnnotationInformation(final Class<?> componentClass, final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex index, final Class<A> annotationType) {
        final HashSet<MethodIdentifier> methodIdentifiers = new HashSet<MethodIdentifier>();
        final Map<Method, List<T>> methods = new HashMap<Method, List<T>>();
        final Map<String, List<T>> classAnnotations = new HashMap<String, List<T>>();

        Class<?> c = componentClass;
        while (c != null && c != Object.class) {

            final ClassReflectionIndex classIndex = index.getClassIndex(c);

            final EEModuleClassDescription description = applicationClasses.getClassByName(c.getName());
            if (description != null) {
                ClassAnnotationInformation<A, T> annotationData = description.getAnnotationInformation(annotationType);
                if (annotationData != null) {

                    if (!annotationData.getClassLevelAnnotations().isEmpty()) {
                        classAnnotations.put(c.getName(), annotationData.getClassLevelAnnotations());
                    }


                    for (Map.Entry<MethodIdentifier, List<T>> entry : annotationData.getMethodLevelAnnotations().entrySet()) {
                        final Method method = classIndex.getMethod(entry.getKey());
                        if (method != null) {
                            //we do not have to worry about private methods being overridden
                            if (Modifier.isPrivate(method.getModifiers()) || !methodIdentifiers.contains(entry.getKey())) {
                                methods.put(method, entry.getValue());
                            }
                        } else {
                            //this should not happen
                            //but if it does, we give some info
                            throw EeLogger.ROOT_LOGGER.cannotResolveMethod(entry.getKey(), c, entry.getValue());
                        }
                    }
                }
            }

            //we store all the method identifiers
            //so we can check if a method is overriden
            for (Method method : (Iterable<Method>)classIndex.getMethods()) {
                //we do not have to worry about private methods being overridden
                if (!Modifier.isPrivate(method.getModifiers())) {
                    methodIdentifiers.add(MethodIdentifier.getIdentifierForMethod(method));
                }
            }

            c = c.getSuperclass();
        }
        return new RuntimeAnnotationInformation<T>(classAnnotations, methods);
    }

    public static <A extends Annotation, T> Set<Method> runtimeAnnotationPresent(final Class<?> componentClass, final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex index, final Class<A> annotationType) {
        RuntimeAnnotationInformation<Object> result = runtimeAnnotationInformation(componentClass, applicationClasses, index, annotationType);
        return result.getMethodAnnotations().keySet();
    }


}

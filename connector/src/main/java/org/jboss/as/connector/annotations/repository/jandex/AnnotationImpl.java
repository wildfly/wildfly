/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.connector.annotations.repository.jandex;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.jca.common.spi.annotations.repository.Annotation;

/**
 *
 * An AnnotationImpl.
 *
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano Maestri</a>
 *
 */
public class AnnotationImpl implements Annotation {
    private final String className;

    private final ClassLoader cl;

    private final List<String> parameterTypes;

    private final String memberName;

    private final boolean onMethod;

    private final boolean onField;

    private final Class<? extends java.lang.annotation.Annotation> annotationClass;

    /**
     * Create a new AnnotationImpl.
     *
     * @param className className
     * @param cl classloader
     * @param parameterTypes parameterTypes
     * @param memberName memberName
     * @param onMethod onMethod
     * @param onField onField
     * @param annotationClass annotationClass
     */
    @SuppressWarnings("unchecked")
    public AnnotationImpl(String className, ClassLoader cl, List<String> parameterTypes, String memberName, boolean onMethod,
            boolean onField, Class<?> annotationClass) {
        super();
        this.className = className;
        this.cl = cl;
        if (parameterTypes != null) {
            this.parameterTypes = new ArrayList<String>(parameterTypes.size());
            this.parameterTypes.addAll(parameterTypes);
        } else {
            this.parameterTypes = new ArrayList<String>(0);
        }

        this.memberName = memberName;
        this.onMethod = onMethod;
        this.onField = onField;
        if (annotationClass.isAnnotation()) {
            this.annotationClass = (Class<? extends java.lang.annotation.Annotation>) annotationClass;
        } else {
            throw ConnectorLogger.ROOT_LOGGER.notAnAnnotation(annotationClass);
        }

    }

    /**
     * Get the className.
     *
     * @return the className.
     */
    @Override
    public final String getClassName() {
        return className;
    }

    /**
     * Get the annotation.
     *
     * @return the annotation.
     */
    @Override
    public final Object getAnnotation() {
        try {
            if (isOnField()) {
                Class<?> clazz = cl.loadClass(className);
                while (!clazz.equals(Object.class)) {
                    try {
                       Field field = clazz.getDeclaredField(memberName);
                       return field.getAnnotation(annotationClass);
                    } catch (Throwable t) {
                       clazz = clazz.getSuperclass();
                    }
                }
            } else if (isOnMethod()) {
                Class<?> clazz = cl.loadClass(className);
                Class<?>[] params = new Class<?>[parameterTypes.size()];
                int i = 0;
                for (String paramClazz : parameterTypes) {
                    params[i] = cl.loadClass(paramClazz);
                    i++;
                }
                while (!clazz.equals(Object.class)) {
                    try {
                       Method method = clazz.getDeclaredMethod(memberName, params);
                       return method.getAnnotation(annotationClass);
                    } catch (Throwable t) {
                       clazz = clazz.getSuperclass();
                    }
                }
            } else { // onClass
                Class<?> clazz = cl.loadClass(className);
                return clazz.getAnnotation(annotationClass);
            }
        } catch (Exception e) {
            ConnectorLogger.ROOT_LOGGER.debug(e.getMessage(), e);
        }

        return null;
    }

    /**
     * Get the parameterTypes.
     *
     * @return the parameterTypes.
     */
    @Override
    public final List<String> getParameterTypes() {
        return Collections.unmodifiableList(parameterTypes);
    }

    /**
     * Get the memberName.
     *
     * @return the memberName.
     */
    @Override
    public final String getMemberName() {
        return memberName;
    }

    /**
     * Get the onMethod.
     *
     * @return the onMethod.
     */
    @Override
    public final boolean isOnMethod() {
        return onMethod;
    }

    /**
     * Get the onField.
     *
     * @return the onField.
     */
    @Override
    public final boolean isOnField() {
        return onField;
    }

}

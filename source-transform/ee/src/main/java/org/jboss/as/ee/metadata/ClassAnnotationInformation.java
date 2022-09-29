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
package org.jboss.as.ee.metadata;

import org.jboss.invocation.proxy.MethodIdentifier;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Class level information about the annotations present on a particular class.
 *
 * @param <A> The annotation type
 * @param <T> The data type that is used to store the annotation information internally
 * @author Stuart Douglas
 */
public final class ClassAnnotationInformation<A extends Annotation, T> {

    private final Class<A> annotationType;
    private final List<T> classLevelAnnotations;
    private final Map<MethodIdentifier, List<T>> methodLevelAnnotations;
    private final Map<String, List<T>> fieldLevelAnnotations;

    ClassAnnotationInformation(final Class<A> annotationType, final List<T> classLevelAnnotations, final Map<MethodIdentifier, List<T>> methodLevelAnnotations, final Map<String, List<T>> fieldLevelAnnotations) {
        this.annotationType = annotationType;
        this.classLevelAnnotations = Collections.unmodifiableList(classLevelAnnotations);
        this.methodLevelAnnotations = Collections.unmodifiableMap(methodLevelAnnotations);
        this.fieldLevelAnnotations = Collections.unmodifiableMap(fieldLevelAnnotations);
    }

    public Class<A> getAnnotationType() {
        return annotationType;
    }

    public List<T> getClassLevelAnnotations() {
        return classLevelAnnotations;
    }

    public Map<String, List<T>> getFieldLevelAnnotations() {
        return fieldLevelAnnotations;
    }

    public Map<MethodIdentifier, List<T>> getMethodLevelAnnotations() {
        return methodLevelAnnotations;
    }
}

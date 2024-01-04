/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.metadata;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Runtime metadata about the annotations that are present on a particular class
 *
 * @author Stuart Douglas
 */
public class RuntimeAnnotationInformation<T> {

    private final Map<String, List<T>> classAnnotations;
    private final Map<Method, List<T>> methodAnnotations;

    public RuntimeAnnotationInformation(final Map<String, List<T>> classAnnotations, final Map<Method, List<T>> methodAnnotations) {
        this.classAnnotations = Collections.unmodifiableMap(classAnnotations);
        this.methodAnnotations = Collections.unmodifiableMap(methodAnnotations);
    }

    public Map<String, List<T>> getClassAnnotations() {
        return classAnnotations;
    }

    public Map<Method, List<T>> getMethodAnnotations() {
        return methodAnnotations;
    }
}

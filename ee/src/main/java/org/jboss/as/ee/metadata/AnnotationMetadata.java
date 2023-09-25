/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.metadata;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

/**
 * Represents a piece of component metadata
 *
 * @author Stuart Douglas
 */
public class AnnotationMetadata<T> {

    private final T componentDefault;

    private final Map<Method, T> methodOverrides;

    public AnnotationMetadata(final T componentDefault, final Map<Method, T> methodOverrides) {
        this.componentDefault = componentDefault;
        this.methodOverrides = Collections.unmodifiableMap(methodOverrides);
    }

    public T getComponentDefault() {
        return componentDefault;
    }

    public Map<Method, T> getMethodOverrides() {
        return methodOverrides;
    }
}

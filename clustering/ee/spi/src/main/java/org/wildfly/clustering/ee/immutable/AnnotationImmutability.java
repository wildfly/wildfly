/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.immutable;

import java.lang.annotation.Annotation;

import org.wildfly.clustering.ee.Immutability;

/**
 * Detects the presence of a specific annotation.
 * @author Paul Ferraro
 */
public class AnnotationImmutability implements Immutability {

    private final Class<? extends Annotation> annotationClass;

    public AnnotationImmutability(Class<? extends Annotation> annotationClass) {
        this.annotationClass = annotationClass;
    }

    @Override
    public boolean test(Object object) {
        return object.getClass().isAnnotationPresent(this.annotationClass);
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component.deployers;

import java.lang.annotation.Annotation;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * An annotation information factory that simply returns true if the annotation is present
 *
 * @author Stuart Douglas
 */
public class BooleanAnnotationInformationFactory<T extends Annotation> extends ClassAnnotationInformationFactory<T, Boolean> {

    public BooleanAnnotationInformationFactory(final Class<T> annotationType) {
        super(annotationType, null);
    }

    @Override
    protected Boolean fromAnnotation(final AnnotationInstance annotationInstance, final PropertyReplacer propertyReplacer) {
        return true;
    }
}

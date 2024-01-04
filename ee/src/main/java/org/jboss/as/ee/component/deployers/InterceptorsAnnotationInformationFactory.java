/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.component.deployers;

import jakarta.interceptor.Interceptors;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.Type;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * @author Stuart Douglas
 */
public class InterceptorsAnnotationInformationFactory extends ClassAnnotationInformationFactory<Interceptors, String[]> {

    protected InterceptorsAnnotationInformationFactory() {
        super(Interceptors.class, null);
    }

    @Override
    protected String[] fromAnnotation(final AnnotationInstance annotationInstance, final PropertyReplacer propertyReplacer) {
        final Type[] classes =  annotationInstance.value().asClassArray();
        final String[] ret = new String[classes.length];
        for(int i = 0; i < classes.length; ++i) {
            ret[i] = classes[i].name().toString();
        }
        return ret;
    }
}

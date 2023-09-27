/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors.annotation;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.metadata.property.PropertyReplacer;

import jakarta.ejb.DependsOn;

/**
 * @author Stuart Douglas
 */
public class DependsOnAnnotationInformationFactory extends ClassAnnotationInformationFactory<DependsOn, String[]> {

    protected DependsOnAnnotationInformationFactory() {
        super(DependsOn.class, null);
    }

    @Override
    protected String[] fromAnnotation(final AnnotationInstance annotationInstance, final PropertyReplacer propertyReplacer) {
        String[] values = annotationInstance.value().asStringArray();
        for (int i = 0; i < values.length; i++) {
            values[i] = propertyReplacer.replaceProperties(values[i]);
        }
        return values;
    }
}

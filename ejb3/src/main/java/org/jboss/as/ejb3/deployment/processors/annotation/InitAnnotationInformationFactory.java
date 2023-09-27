/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors.annotation;

import jakarta.ejb.Init;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * @author Stuart Douglas
 */
public class InitAnnotationInformationFactory extends ClassAnnotationInformationFactory<Init, String> {

    protected InitAnnotationInformationFactory() {
        super(Init.class, null);
    }

    @Override
    protected String fromAnnotation(final AnnotationInstance annotationInstance, final PropertyReplacer propertyReplacer) {
        AnnotationValue value = annotationInstance.value();
        if (value == null) {
            return null;
        }
        return propertyReplacer.replaceProperties(value.asString());
    }
}

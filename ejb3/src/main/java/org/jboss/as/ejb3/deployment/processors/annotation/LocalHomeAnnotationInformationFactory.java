/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors.annotation;

import jakarta.ejb.LocalHome;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * Processes the {@link jakarta.ejb.LocalHome} annotation on a session bean
 *
 * @author Stuart Douglas
 */
public class LocalHomeAnnotationInformationFactory extends ClassAnnotationInformationFactory<LocalHome, String> {

    protected LocalHomeAnnotationInformationFactory() {
        super(LocalHome.class, null);
    }

    @Override
    protected String fromAnnotation(final AnnotationInstance annotationInstance, final PropertyReplacer propertyReplacer) {
        AnnotationValue value = annotationInstance.value();
        return value.asClass().toString();
    }
}

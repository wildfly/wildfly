/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors.annotation;

import jakarta.annotation.security.RunAs;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * Processes the {@link jakarta.annotation.security.RunAs} annotation on a session bean
 *
 * @author Stuart Douglas
 */
public class RunAsAnnotationInformationFactory extends ClassAnnotationInformationFactory<RunAs, String> {

    protected RunAsAnnotationInformationFactory() {
        super(RunAs.class, null);
    }

    @Override
    protected String fromAnnotation(final AnnotationInstance annotationInstance, final PropertyReplacer propertyReplacer) {
        return propertyReplacer.replaceProperties(annotationInstance.value().asString());
    }
}

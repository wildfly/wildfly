/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors.annotation;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.ejb3.annotation.RunAsPrincipal;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * Processes the {@link org.jboss.ejb3.annotation.RunAsPrincipal} annotation on a session bean
 *
 * @author Guillaume Grossetie
 * @author Anil Saldhana
 */
public class RunAsPrincipalAnnotationInformationFactory extends ClassAnnotationInformationFactory<RunAsPrincipal, String> {

    protected RunAsPrincipalAnnotationInformationFactory() {
        super(RunAsPrincipal.class, null);
    }

    @Override
    protected String fromAnnotation(final AnnotationInstance annotationInstance, final PropertyReplacer propertyReplacer) {
        return propertyReplacer.replaceProperties(annotationInstance.value().asString());
    }
}

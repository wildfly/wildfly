/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors.annotation;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.metadata.property.PropertyReplacer;

import jakarta.ejb.Remove;

/**
 * Processes the {@link jakarta.ejb.Remove}
 *
 * @author Stuart Douglas
 */
public class RemoveAnnotationInformationFactory extends ClassAnnotationInformationFactory<Remove, Boolean> {

    protected RemoveAnnotationInformationFactory() {
        super(Remove.class, null);
    }

    @Override
    protected Boolean fromAnnotation(final AnnotationInstance annotationInstance, final PropertyReplacer propertyReplacer) {
        AnnotationValue value = annotationInstance.value("retainIfException");
        if(value == null) {
            return false;
        }
        return value.asBoolean();
    }
}

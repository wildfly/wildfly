/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors.annotation;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.ejb3.annotation.DeliveryActive;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * Processes the {@link org.jboss.ejb3.annotation.DeliveryActive} annotation
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class DeliveryActiveAnnotationInformationFactory extends ClassAnnotationInformationFactory<DeliveryActive, Boolean> {

    protected DeliveryActiveAnnotationInformationFactory() {
        super(DeliveryActive.class, null);
    }

    @Override
    protected Boolean fromAnnotation(final AnnotationInstance annotationInstance, final PropertyReplacer propertyReplacer) {
        final AnnotationValue value = annotationInstance.value();
        if (value == null) {
            return Boolean.TRUE;
        }
        return value.asBoolean();
    }
}

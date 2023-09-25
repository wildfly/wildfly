/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors.annotation;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.ejb3.annotation.DeliveryGroup;
import org.jboss.ejb3.annotation.DeliveryGroups;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * Processes the {@link DeliveryGroup} annotation
 *
 * @author Flavia Rainone
 */
public class DeliveryGroupAnnotationInformationFactory extends ClassAnnotationInformationFactory<DeliveryGroup, String> {

    protected DeliveryGroupAnnotationInformationFactory() {
        super(DeliveryGroup.class, DeliveryGroups.class);
    }

    @Override
    protected String fromAnnotation(final AnnotationInstance annotationInstance, final PropertyReplacer propertyReplacer) {
        return annotationInstance.value().asString();
    }
}

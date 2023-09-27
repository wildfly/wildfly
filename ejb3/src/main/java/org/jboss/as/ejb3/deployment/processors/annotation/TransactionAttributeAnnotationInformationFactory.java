/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors.annotation;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.metadata.property.PropertyReplacer;

import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Processes the {@link jakarta.ejb.TransactionAttribute} annotation on a session bean
 * @author Stuart Douglas
 */
public class TransactionAttributeAnnotationInformationFactory extends ClassAnnotationInformationFactory<TransactionAttribute, TransactionAttributeType> {

    protected TransactionAttributeAnnotationInformationFactory() {
        super(TransactionAttribute.class, null);
    }

    @Override
    protected TransactionAttributeType fromAnnotation(final AnnotationInstance annotationInstance, final PropertyReplacer propertyReplacer) {

        final AnnotationValue value = annotationInstance.value();
        if(value == null) {
            return TransactionAttributeType.REQUIRED;
        }
        return TransactionAttributeType.valueOf(value.asEnum());
    }
}

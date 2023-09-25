/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors.annotation;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.metadata.property.PropertyReplacer;

import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;

/**
 * Processes the {@link jakarta.ejb.TransactionManagementType} annotation on a session bean
 *
 * @author Stuart Douglas
 */
public class TransactionManagementAnnotationInformationFactory extends ClassAnnotationInformationFactory<TransactionManagement, TransactionManagementType> {

    protected TransactionManagementAnnotationInformationFactory() {
        super(TransactionManagement.class, null);
    }

    @Override
    protected TransactionManagementType fromAnnotation(final AnnotationInstance annotationInstance, final PropertyReplacer propertyReplacer) {
        final AnnotationValue value = annotationInstance.value();
        if(value == null) {
            return TransactionManagementType.CONTAINER;
        }
        return TransactionManagementType.valueOf(value.asEnum());
    }
}

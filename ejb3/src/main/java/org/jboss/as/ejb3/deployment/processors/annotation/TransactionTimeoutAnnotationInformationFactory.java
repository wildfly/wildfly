/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors.annotation;

import java.util.concurrent.TimeUnit;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.ejb3.annotation.TransactionTimeout;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.metadata.property.PropertyReplacer;

public class TransactionTimeoutAnnotationInformationFactory extends ClassAnnotationInformationFactory<TransactionTimeout, Integer> {

    protected TransactionTimeoutAnnotationInformationFactory() {
        super(TransactionTimeout.class, null);
    }

    @Override
    protected Integer fromAnnotation(final AnnotationInstance annotationInstance, final PropertyReplacer propertyReplacer) {
        final long timeout = annotationInstance.value().asLong();
        AnnotationValue unitAnnVal = annotationInstance.value("unit");
        final TimeUnit unit = unitAnnVal != null ? TimeUnit.valueOf(unitAnnVal.asEnum()) : TimeUnit.SECONDS;
        return (int) unit.toSeconds(timeout);
    }
}
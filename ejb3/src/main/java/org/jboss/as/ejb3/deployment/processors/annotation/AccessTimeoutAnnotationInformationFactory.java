/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors.annotation;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.as.ejb3.concurrency.AccessTimeoutDetails;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.metadata.property.PropertyReplacer;

import jakarta.ejb.AccessTimeout;
import java.util.concurrent.TimeUnit;

/**
 * Processes the {@link jakarta.ejb.AccessTimeout} annotation on a session bean
 *
 * @author Stuart Douglas
 */
public class AccessTimeoutAnnotationInformationFactory extends ClassAnnotationInformationFactory<AccessTimeout, AccessTimeoutDetails> {

    protected AccessTimeoutAnnotationInformationFactory() {
        super(AccessTimeout.class, null);
    }

    @Override
    protected AccessTimeoutDetails fromAnnotation(final AnnotationInstance annotationInstance, final PropertyReplacer propertyReplacer) {
        final long timeout = annotationInstance.value().asLong();
        AnnotationValue unitAnnVal = annotationInstance.value("unit");
        final TimeUnit unit = unitAnnVal != null ? TimeUnit.valueOf(unitAnnVal.asEnum()) : TimeUnit.MILLISECONDS;
        return new AccessTimeoutDetails(timeout, unit);
    }
}

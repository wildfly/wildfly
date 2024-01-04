/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors.annotation;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.as.ejb3.component.stateful.StatefulTimeoutInfo;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.metadata.property.PropertyReplacer;

import jakarta.ejb.StatefulTimeout;
import java.util.concurrent.TimeUnit;

/**
 * @author Stuart Douglas
 */
public class StatefulTimeoutAnnotationInformationFactory extends ClassAnnotationInformationFactory<StatefulTimeout, StatefulTimeoutInfo> {

    protected StatefulTimeoutAnnotationInformationFactory() {
        super(StatefulTimeout.class, null);
    }

    @Override
    protected StatefulTimeoutInfo fromAnnotation(final AnnotationInstance annotationInstance, final PropertyReplacer propertyReplacer) {
        final long value = annotationInstance.value().asLong();
        final AnnotationValue unitValue = annotationInstance.value("unit");
        final TimeUnit unit;
        if (unitValue != null) {
            unit = TimeUnit.valueOf(unitValue.asEnum());
        } else {
            unit = TimeUnit.MINUTES;
        }
        return new StatefulTimeoutInfo(value, unit);
    }
}

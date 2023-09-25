/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.deployment.processors.annotation;


import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.ejb3.annotation.Pool;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * Processes {@link Pool} annotation on EJB classes
 *
 * @author Jaikiran Pai
 */
public class PoolAnnotationInformationFactory extends ClassAnnotationInformationFactory<Pool, String> {

    public PoolAnnotationInformationFactory() {
        super(Pool.class, null);
    }

    @Override
    protected String fromAnnotation(final AnnotationInstance annotationInstance, final PropertyReplacer propertyReplacer) {
        AnnotationValue value = annotationInstance.value();
        if (value == null || value.asString().isEmpty()) {
            throw EeLogger.ROOT_LOGGER.annotationAttributeMissing("@Pool", "value");
        }
        return propertyReplacer.replaceProperties(value.asString());
    }
}

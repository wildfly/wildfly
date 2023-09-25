/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors.annotation;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.ejb3.annotation.ClusteredSingleton;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * Processes the {@link ClusteredSingleton} annotation
 *
 * @author Flavia Rainone
 */
public class ClusteredSingletonAnnotationInformationFactory extends ClassAnnotationInformationFactory<ClusteredSingleton, Boolean> {

    protected ClusteredSingletonAnnotationInformationFactory() {
        super(ClusteredSingleton.class, null);
    }

    @Override
    protected Boolean fromAnnotation(AnnotationInstance annotationInstance, PropertyReplacer propertyReplacer) {
        return Boolean.TRUE;
    }
}

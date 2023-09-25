/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors.annotation;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * Processes the {@link org.jboss.ejb3.annotation.ResourceAdapter} annotation
 *
 * @author Stuart Douglas
 */
public class ResourceAdaptorAnnotationInformationFactory extends ClassAnnotationInformationFactory<org.jboss.ejb3.annotation.ResourceAdapter, String> {

    protected ResourceAdaptorAnnotationInformationFactory() {
        super(org.jboss.ejb3.annotation.ResourceAdapter.class, null);
    }

    @Override
    protected String fromAnnotation(final AnnotationInstance annotationInstance, final PropertyReplacer propertyReplacer) {
        String resourceAdapterValue = annotationInstance.value().asString();
        return propertyReplacer.replaceProperties(resourceAdapterValue);
    }
}

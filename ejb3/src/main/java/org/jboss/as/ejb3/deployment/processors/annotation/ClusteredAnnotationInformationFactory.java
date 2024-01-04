/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors.annotation;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.ejb3.annotation.Clustered;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * @author Paul Ferraro
 */
@Deprecated
public class ClusteredAnnotationInformationFactory extends ClassAnnotationInformationFactory<Clustered, Void> {

    protected ClusteredAnnotationInformationFactory() {
        super(Clustered.class, null);
    }

    @Override
    protected Void fromAnnotation(AnnotationInstance annotationInstance, PropertyReplacer propertyReplacer) {
        EjbLogger.DEPLOYMENT_LOGGER.deprecatedAnnotation(Clustered.class.getSimpleName());
        return null;
    }
}

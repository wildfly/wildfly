/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors.annotation;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.metadata.property.PropertyReplacer;

import jakarta.ejb.Lock;
import jakarta.ejb.LockType;

/**
 * Processes the {@link jakarta.ejb.Lock} annotation on a session bean, which allows concurrent access (like @Singleton and @Stateful beans),
 * and its methods and updates the {@link org.jboss.as.ejb3.component.session.SessionBeanComponentDescription} accordingly.
 *
 * @author Stuart Douglas
 */
public class LockAnnotationInformationFactory extends ClassAnnotationInformationFactory<Lock, LockType> {

    protected LockAnnotationInformationFactory() {
        super(Lock.class, null);
    }

    @Override
    protected LockType fromAnnotation(final AnnotationInstance annotationInstance, final PropertyReplacer propertyReplacer) {
        AnnotationValue value = annotationInstance.value();
        if(value == null) {
            return LockType.WRITE;
        }
        return LockType.valueOf(annotationInstance.value().asEnum());
    }
}

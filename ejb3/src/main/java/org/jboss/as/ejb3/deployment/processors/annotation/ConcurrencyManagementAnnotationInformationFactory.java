/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors.annotation;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.metadata.property.PropertyReplacer;

import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;

/**
 * Processes the {@link jakarta.ejb.ConcurrencyManagement} annotation on a session bean
 *
 * @author Stuart Douglas
 */
public class ConcurrencyManagementAnnotationInformationFactory extends ClassAnnotationInformationFactory<ConcurrencyManagement, ConcurrencyManagementType> {

    protected ConcurrencyManagementAnnotationInformationFactory() {
        super(ConcurrencyManagement.class, null);
    }

    @Override
    protected ConcurrencyManagementType fromAnnotation(final AnnotationInstance annotationInstance, final PropertyReplacer propertyReplacer) {
        final AnnotationValue value = annotationInstance.value();
        if(value == null) {
            return ConcurrencyManagementType.CONTAINER;
        }
        return ConcurrencyManagementType.valueOf(value.asEnum());
    }
}

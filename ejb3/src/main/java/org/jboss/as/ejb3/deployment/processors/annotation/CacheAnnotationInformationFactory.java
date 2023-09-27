/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors.annotation;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.as.ejb3.cache.CacheInfo;
import org.jboss.ejb3.annotation.Cache;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * @author Paul Ferraro
 */
public class CacheAnnotationInformationFactory extends ClassAnnotationInformationFactory<Cache, CacheInfo> {

    protected CacheAnnotationInformationFactory() {
        super(Cache.class, null);
    }

    @Override
    protected CacheInfo fromAnnotation(AnnotationInstance annotationInstance, PropertyReplacer propertyReplacer) {
        String value = annotationInstance.value().asString();
        return new CacheInfo(propertyReplacer.replaceProperties(value));
    }
}

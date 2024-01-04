/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.annotated.slim.backed;

import org.jboss.weld.annotated.slim.AnnotatedTypeIdentifier;
import org.jboss.weld.annotated.slim.backed.BackedAnnotatedType;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.ClassTransformer;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedTypeMarshaller;

/**
 * Validates marshalling of {@link BackedAnnotatedType}.
 * @author Paul Ferraro
 */
public class BackedAnnotatedTypeMarshaller<X> extends AnnotatedTypeMarshaller<X, BackedAnnotatedType<X>> {

    @SuppressWarnings("unchecked")
    public BackedAnnotatedTypeMarshaller() {
        super((Class<BackedAnnotatedType<X>>) (Class<?>) BackedAnnotatedType.class);
    }

    @Override
    protected BackedAnnotatedType<X> getAnnotatedType(AnnotatedTypeIdentifier identifier, BeanManagerImpl manager) {
        BackedAnnotatedType<X> result = super.getAnnotatedType(identifier, manager);
        // If type is not yet know, attempt to load it
        if (result == null) {
            @SuppressWarnings("unchecked")
            Class<X> targetClass = (Class<X>) manager.getServices().get(ResourceLoader.class).classForName(identifier.getClassName());
            result = manager.getServices().get(ClassTransformer.class).getBackedAnnotatedType(targetClass, identifier.getBdaId());
        }
        return result;
    }
}

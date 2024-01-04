/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.annotated.slim;

import java.lang.reflect.Constructor;

import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedType;

import org.jboss.weld.annotated.slim.SlimAnnotatedType;
import org.jboss.weld.util.reflection.DeclaredMemberIndexer;

/**
 * Generic marshaller for an {@link AnnotatedConstructor}.
 * @author Paul Ferraro
 */
public class AnnotatedConstructorMarshaller<X, T extends SlimAnnotatedType<X>, C extends AnnotatedConstructor<X>> extends AnnotatedCallableMarshaller<X, Constructor<X>, T, AnnotatedConstructor<X>, C> {

    public AnnotatedConstructorMarshaller(Class<C> targetClass, Class<T> typeClass) {
        super(targetClass, typeClass, DeclaredMemberIndexer::getConstructorForIndex, AnnotatedType::getConstructors, DeclaredMemberIndexer::getIndexForConstructor);
    }
}

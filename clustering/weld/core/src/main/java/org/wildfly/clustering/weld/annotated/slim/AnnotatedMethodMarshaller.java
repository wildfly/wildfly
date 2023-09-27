/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.annotated.slim;

import java.lang.reflect.Method;

import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;

import org.jboss.weld.annotated.slim.SlimAnnotatedType;
import org.jboss.weld.util.reflection.DeclaredMemberIndexer;

/**
 * Generic marshaller for an {@link AnnotatedMethod}.
 * @author Paul Ferraro
 */
public class AnnotatedMethodMarshaller<X, T extends SlimAnnotatedType<X>, M extends AnnotatedMethod<X>> extends AnnotatedCallableMarshaller<X, Method, T, AnnotatedMethod<? super X>, M> {

    public AnnotatedMethodMarshaller(Class<M> targetClass, Class<T> typeClass) {
        super(targetClass, typeClass, DeclaredMemberIndexer::getMethodForIndex, AnnotatedType::getMethods, DeclaredMemberIndexer::getIndexForMethod);
    }
}

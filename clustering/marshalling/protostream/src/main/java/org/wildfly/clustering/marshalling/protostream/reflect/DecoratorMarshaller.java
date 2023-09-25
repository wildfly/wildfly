/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.reflect;

import java.util.function.UnaryOperator;

/**
 * Marshaller for a decorator that does not provide public access to its decorated object.
 * @author Paul Ferraro
 */
public class DecoratorMarshaller<T>  extends UnaryFieldMarshaller<T, T> {

    public DecoratorMarshaller(Class<T> decoratedClass, UnaryOperator<T> decorator, T sample) {
        this(decorator.apply(sample).getClass().asSubclass(decoratedClass), decoratedClass, decorator);
    }

    private DecoratorMarshaller(Class<? extends T> decoratorClass, Class<T> decoratedClass, UnaryOperator<T> decorator) {
        super(decoratorClass, decoratedClass, decorator);
    }
}
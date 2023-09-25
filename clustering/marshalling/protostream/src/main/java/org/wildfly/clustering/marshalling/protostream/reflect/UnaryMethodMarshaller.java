/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.reflect;

import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * Generic marshaller based on a single non-public accessor method.
 * @author Paul Ferraro
 */
public class UnaryMethodMarshaller<T, M> extends UnaryMemberMarshaller<T, Method, M> {

    public UnaryMethodMarshaller(Class<? extends T> targetClass, Class<M> fieldClass, Function<M, T> factory) {
        super(targetClass, Reflect::invoke, Reflect::findMethod, fieldClass, factory);
    }
}

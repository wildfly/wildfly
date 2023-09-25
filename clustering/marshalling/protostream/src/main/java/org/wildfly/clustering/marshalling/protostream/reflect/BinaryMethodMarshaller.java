/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.reflect;

import java.lang.reflect.Method;
import java.util.function.BiFunction;

/**
 * Generic marshaller based on two non-public accessor methods.
 * @author Paul Ferraro
 */
public class BinaryMethodMarshaller<T, M1, M2> extends BinaryMemberMarshaller<T, Method, M1, M2> {

    public BinaryMethodMarshaller(Class<? extends T> type, Class<M1> member1Type, Class<M2> member2Type, BiFunction<M1, M2, T> factory) {
        super(type, Reflect::invoke, Reflect::findMethod, member1Type, member2Type, factory);
    }
}
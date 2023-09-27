/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.reflect;

import java.lang.reflect.Method;

/**
 * Generic marshaller based on three non-public accessor methods.
 * @author Paul Ferraro
 */
public class TernaryMethodMarshaller<T, M1, M2, M3> extends TernaryMemberMarshaller<T, Method, M1, M2, M3> {

    public TernaryMethodMarshaller(Class<? extends T> type, Class<M1> member1Type, Class<M2> member2Type, Class<M3> member3Type, TriFunction<M1, M2, M3, T> factory) {
        super(type, Reflect::invoke, Reflect::findMethod, member1Type, member2Type, member3Type, factory);
    }
}

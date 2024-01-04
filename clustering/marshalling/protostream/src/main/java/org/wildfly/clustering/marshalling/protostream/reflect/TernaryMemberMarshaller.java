/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.reflect;

import java.lang.reflect.Member;
import java.util.function.BiFunction;

/**
 * Generic marshaller based on three non-public members.
 * @author Paul Ferraro
 */
public class TernaryMemberMarshaller<T, M extends Member, M1, M2, M3> extends AbstractMemberMarshaller<T, M> {

    private final Class<M1> member1Type;
    private final Class<M2> member2Type;
    private final Class<M3> member3Type;
    private final TriFunction<M1, M2, M3, T> factory;

    public TernaryMemberMarshaller(Class<? extends T> type, BiFunction<Object, M, Object> accessor, BiFunction<Class<?>, Class<?>, M> memberLocator, Class<M1> member1Type, Class<M2> member2Type, Class<M3> member3Type, TriFunction<M1, M2, M3, T> factory) {
        super(type, accessor, memberLocator, member1Type, member2Type, member3Type);
        this.member1Type = member1Type;
        this.member2Type = member2Type;
        this.member3Type = member3Type;
        this.factory = factory;
    }

    @Override
    public T apply(Object[] parameters) {
        return this.factory.apply(this.member1Type.cast(parameters[0]), this.member2Type.cast(parameters[1]), this.member3Type.cast(parameters[2]));
    }
}

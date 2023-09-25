/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.reflect;

import java.lang.reflect.Member;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Generic marshaller based on a single non-public member.
 * @author Paul Ferraro
 */
public class UnaryMemberMarshaller<T, M extends Member, M1> extends AbstractMemberMarshaller<T, M> {

    private final Class<M1> memberType;
    private final Function<M1, T> factory;

    public UnaryMemberMarshaller(Class<? extends T> type, BiFunction<Object, M, Object> accessor, BiFunction<Class<?>, Class<?>, M> memberLocator, Class<M1> memberType, Function<M1, T> factory) {
        super(type, accessor, memberLocator, memberType);
        this.memberType = memberType;
        this.factory = factory;
    }

    @Override
    public T apply(Object[] parameters) {
        return this.factory.apply(this.memberType.cast(parameters[0]));
    }
}

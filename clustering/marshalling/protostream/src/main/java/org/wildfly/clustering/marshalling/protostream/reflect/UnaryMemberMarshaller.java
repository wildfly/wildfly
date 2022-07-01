/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

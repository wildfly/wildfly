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

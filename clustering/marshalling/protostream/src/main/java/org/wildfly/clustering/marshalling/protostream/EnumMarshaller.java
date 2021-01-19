/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.protostream;

import java.util.OptionalInt;

import org.infinispan.protostream.ImmutableSerializationContext;

import protostream.com.google.protobuf.CodedOutputStream;

/**
 * ProtoStream marshaller for enums.
 * @author Paul Ferraro
 */
public class EnumMarshaller<E extends Enum<E>> implements org.infinispan.protostream.EnumMarshaller<E>, Predictable<E> {

    private final Class<E> enumClass;
    private final E[] values;

    public EnumMarshaller(Class<E> enumClass) {
        this.enumClass = enumClass;
        this.values = enumClass.getEnumConstants();
    }

    @Override
    public Class<? extends E> getJavaClass() {
        return this.enumClass;
    }

    @Override
    public String getTypeName() {
        Package targetPackage = this.enumClass.getPackage();
        return (targetPackage != null) ? (targetPackage.getName() + '.' + this.enumClass.getSimpleName()) : this.enumClass.getSimpleName();
    }

    @Override
    public E decode(int ordinal) {
        return this.values[ordinal];
    }

    @Override
    public int encode(E value) throws IllegalArgumentException {
        return value.ordinal();
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, E value) {
        return OptionalInt.of(CodedOutputStream.computeEnumSizeNoTag(value.ordinal()));
    }
}

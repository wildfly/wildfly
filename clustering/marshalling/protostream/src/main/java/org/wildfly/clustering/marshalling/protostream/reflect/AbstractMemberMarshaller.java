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

import java.io.IOException;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Generic marshaller based on non-public members.
 * @author Paul Ferraro
 */
public abstract class AbstractMemberMarshaller<T, M extends Member> implements ProtoStreamMarshaller<T>, Function<Object[], T> {

    private final Class<? extends T> type;
    private final BiFunction<Object, M, Object> accessor;
    private final List<M> members;

    public AbstractMemberMarshaller(Class<? extends T> type, BiFunction<Object, M, Object> accessor, BiFunction<Class<?>, Class<?>, M> memberLocator, Class<?>... memberTypes) {
        this.type = type;
        this.accessor = accessor;
        this.members = new ArrayList<>(memberTypes.length);
        for (Class<?> memberType : memberTypes) {
            this.members.add(memberLocator.apply(type, memberType));
        }
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return this.type;
    }

    @Override
    public T readFrom(ProtoStreamReader reader) throws IOException {
        Object[] values = new Object[this.members.size()];
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            if ((index > 0) || (index <= values.length)) {
                values[index - 1] = reader.readAny();
            } else {
                reader.skipField(tag);
            }
        }
        return this.apply(values);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, T source) throws IOException {
        for (int i = 0; i < this.members.size(); ++i) {
            Object value = this.accessor.apply(source, this.members.get(i));
            if (value != null) {
                writer.writeAny(i + 1, value);
            }
        }
    }
}

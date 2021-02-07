/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

/**
 * Marshaller for an object whose fields are marshalled via a {@link FieldSetMarshaller} and constructed via a {@link ProtoStreamBuilder}.
 * @author Paul Ferraro
 * @param <T> the type of this marshaller
 * @param <B> the builder type used by this marshaller
 */
public class ProtoStreamBuilderFieldSetMarshaller<T, B extends ProtoStreamBuilder<T>> extends FunctionalFieldSetMarshaller<T, B> {

    @SuppressWarnings("unchecked")
    public ProtoStreamBuilderFieldSetMarshaller(FieldSetMarshaller<T, B> marshaller) {
        super((Class<T>) marshaller.getBuilder().build().getClass(), marshaller, B::build);
    }

    public ProtoStreamBuilderFieldSetMarshaller(Class<? extends T> targetClass, FieldSetMarshaller<T, B> marshaller) {
        super(targetClass, marshaller, B::build);
    }
}

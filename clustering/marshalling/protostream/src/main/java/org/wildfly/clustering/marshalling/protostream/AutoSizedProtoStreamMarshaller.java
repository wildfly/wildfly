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

import java.io.IOException;
import java.util.OptionalInt;

import org.infinispan.protostream.ImmutableSerializationContext;

/**
 * A marshaller whose size is auto-computed.
 * @author Paul Ferraro
 */
public interface AutoSizedProtoStreamMarshaller<T> extends ProtoStreamMarshaller<T> {

    @Override
    default OptionalInt size(ImmutableSerializationContext context, T value) {
        SizeComputingWriter writer = new SizeComputingWriter();
        try {
            this.writeTo(context, writer, value);
            return OptionalInt.of(writer.getAsInt());
        } catch (IOException e) {
            return OptionalInt.empty();
        }
    }
}

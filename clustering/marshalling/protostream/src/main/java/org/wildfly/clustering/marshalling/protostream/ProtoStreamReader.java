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

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;

/**
 * A {@link RawProtoStreamReader} with the additional ability to read an arbitrary embedded object.
 * @author Paul Ferraro
 */
public interface ProtoStreamReader extends RawProtoStreamReader {

    ImmutableSerializationContext getSerializationContext();

    <T> T readObject(Class<T> targetClass) throws IOException;

    <E extends Enum<E>> E readEnum(Class<E> enumClass) throws IOException;

    /**
     * Ignores the field with the specified tag.
     * @param tag a field tag
     * @return true, if the caller should continue reading the stream, false otherwise.
     * @throws IOException if the field could not be skipped.
     */
    default boolean ignoreField(int tag) throws IOException {
        return (tag != 0) && this.skipField(tag);
    }
}

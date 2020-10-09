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

import java.io.IOException;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;

/**
 * Resolver for class instances.
 * @author Paul Ferraro
 */
public interface ClassResolver extends Predictable<Class<?>> {
    /**
     * Writes any additional context necessary to resolve the specified class.
     * @param context a serialization context
     * @param writer a ProtoStream writer
     * @param targetClass the class to be annotated
     * @throws IOException
     */
    void annotate(ImmutableSerializationContext context, RawProtoStreamWriter writer, Class<?> targetClass) throws IOException;

    /**
     * Resolves a class with the specified name from the specified reader.
     * @param context a serialization context
     * @param reader a ProtoStream reader
     * @param className a class name
     * @return a resolved class instance
     * @throws IOException
     */
    Class<?> resolve(ImmutableSerializationContext context, RawProtoStreamReader reader, String className) throws IOException;
}

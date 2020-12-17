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
import java.io.InvalidClassException;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;

/**
 * Resolves classes from a specific {@link ClassLoader}.
 * @author Paul Ferraro
 */
public class ClassLoaderResolver implements ClassResolver {

    private final ClassLoader loader;

    public ClassLoaderResolver(ClassLoader loader) {
        this.loader = loader;
    }

    @Override
    public void annotate(ImmutableSerializationContext context, RawProtoStreamWriter writer, Class<?> targetClass) throws IOException {
        // Nothing to annotate
    }

    @Override
    public Class<?> resolve(ImmutableSerializationContext context, RawProtoStreamReader reader, String className) throws IOException {
        try {
            return this.loader.loadClass(className);
        } catch (ClassNotFoundException e) {
            InvalidClassException exception = new InvalidClassException(e.getMessage());
            exception.initCause(e);
            throw exception;
        }
    }
}

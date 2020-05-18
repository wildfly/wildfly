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
import java.io.InvalidObjectException;
import java.io.StreamCorruptedException;
import java.nio.charset.StandardCharsets;
import java.util.OptionalInt;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;

/**
 * @author Paul Ferraro
 */
public enum ModuleMarshaller implements ProtoStreamMarshaller<Module> {
    INSTANCE;

    @Override
    public Module readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        String moduleName = new String(reader.readByteArray(), StandardCharsets.UTF_8);
        if (reader.readTag() != 0) {
            throw new StreamCorruptedException();
        }
        try {
            return Module.getBootModuleLoader().loadModule(moduleName);
        } catch (ModuleLoadException e) {
            InvalidObjectException exception = new InvalidObjectException(e.getMessage());
            exception.initCause(e);
            throw exception;
        }
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Module module) throws IOException {
        byte[] name = module.getName().getBytes(StandardCharsets.UTF_8);
        writer.writeUInt32NoTag(name.length);
        writer.writeRawBytes(name, 0, name.length);
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, Module value) {
        return OptionalInt.of(Predictable.stringSize(value.getName()));
    }

    @Override
    public Class<? extends Module> getJavaClass() {
        return Module.class;
    }
}

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

import org.infinispan.protostream.descriptors.WireType;

/**
 * @author Paul Ferraro
 */
public enum StackTraceElementMarshaller implements ProtoStreamMarshaller<StackTraceElement> {
    INSTANCE;

    private static final int CLASS_NAME_INDEX = 1;
    private static final int METHOD_NAME_INDEX = 2;
    private static final int FILE_NAME_INDEX = 3;
    private static final int LINE_NUMBER_INDEX = 4;

    @Override
    public StackTraceElement readFrom(ProtoStreamReader reader) throws IOException {
        String className = null;
        String methodName = null;
        String fileName = null;
        int line = -1;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case CLASS_NAME_INDEX:
                    className = reader.readAny(String.class);
                    break;
                case METHOD_NAME_INDEX:
                    methodName = reader.readAny(String.class);
                    break;
                case FILE_NAME_INDEX:
                    fileName = reader.readAny(String.class);
                    break;
                case LINE_NUMBER_INDEX:
                    line = reader.readUInt32();
                    if (line == 0) {
                        // Native method
                        line = -2;
                    }
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new StackTraceElement(className, methodName, fileName, line);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, StackTraceElement element) throws IOException {
        writer.writeAny(CLASS_NAME_INDEX, element.getClassName());
        writer.writeAny(METHOD_NAME_INDEX, element.getMethodName());
        String fileName = element.getFileName();
        if (fileName != null) {
            writer.writeAny(FILE_NAME_INDEX, fileName);
        }
        int line = element.getLineNumber();
        boolean nativeMethod = element.isNativeMethod();
        if (nativeMethod || line > 0) {
            writer.writeUInt32(LINE_NUMBER_INDEX, nativeMethod ? 0 : line);
        }
    }

    @Override
    public Class<? extends StackTraceElement> getJavaClass() {
        return StackTraceElement.class;
    }
}

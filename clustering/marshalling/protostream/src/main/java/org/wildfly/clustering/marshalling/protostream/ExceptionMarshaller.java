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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.OptionalInt;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;

import protostream.com.google.protobuf.CodedOutputStream;

/**
 * Generic marshaller for a Throwable.
 * @author Paul Ferraro
 */
public class ExceptionMarshaller<E extends Throwable> implements ProtoStreamMarshaller<E> {

    private final Class<E> exceptionClass;
    private final Constructor<E> emptyConstructor;
    private final Constructor<E> messageConstructor;
    private final Constructor<E> causeConstructor;
    private final Constructor<E> messageCauseConstructor;

    public ExceptionMarshaller(Class<E> exceptionClass) {
        this.exceptionClass = exceptionClass;
        this.emptyConstructor = this.getConstructor();
        this.messageConstructor = this.getConstructor(String.class);
        this.causeConstructor = this.getConstructor(Throwable.class);
        this.messageCauseConstructor = this.getConstructor(String.class, Throwable.class);
    }

    private Constructor<E> getConstructor(Class<?>... parameterTypes) {
        try {
            return this.exceptionClass.getConstructor(parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @Override
    public Class<? extends E> getJavaClass() {
        return this.exceptionClass;
    }

    @Override
    public E readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        String message = AnyField.STRING.cast(String.class).readFrom(context, reader);

        Throwable cause = (Throwable) ObjectMarshaller.INSTANCE.readFrom(context, reader);

        E exception = this.createException(message, cause);

        int stackTraceSize = reader.readUInt32();
        if (stackTraceSize > 0) {
            StackTraceElement[] stackTrace = new StackTraceElement[stackTraceSize];
            for (int i = 0; i < stackTraceSize; ++i) {
                String className = (String) ObjectMarshaller.INSTANCE.readFrom(context, reader);
                String methodName = (String) ObjectMarshaller.INSTANCE.readFrom(context, reader);
                String fileName = (String) ObjectMarshaller.INSTANCE.readFrom(context, reader);
                int lineNumber = reader.readUInt32();
                stackTrace[i] = new StackTraceElement(className, methodName, fileName, lineNumber);
            }
            exception.setStackTrace(stackTrace);
        }

        int suppressed = reader.readUInt32();
        for (int i = 0; i < suppressed; ++i) {
            exception.addSuppressed((Throwable) ObjectMarshaller.INSTANCE.readFrom(context, reader));
        }

        return exception;
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, E exception) throws IOException {
        String message = exception.getMessage();
        Throwable cause = exception.getCause();
        // Avoid serializing redundant message
        AnyField.STRING.writeTo(context, writer, (cause == null) || !cause.toString().equals(message) ? message : null);

        ObjectMarshaller.INSTANCE.writeTo(context, writer, exception.getCause());

        StackTraceElement[] stackTrace = exception.getStackTrace();
        writer.writeUInt32NoTag(stackTrace.length);
        for (StackTraceElement element : stackTrace) {
            for (String elementField : new String[] { element.getClassName(), element.getMethodName(), element.getFileName() }) {
                // Marshal as objects not strings, to leverage reference sharing.
                ObjectMarshaller.INSTANCE.writeTo(context, writer, elementField);
            }
            writer.writeUInt32NoTag(element.getLineNumber());
        }

        Throwable[] suppressed = exception.getSuppressed();
        writer.writeUInt32NoTag(suppressed.length);
        for (Throwable suppression : suppressed) {
            ObjectMarshaller.INSTANCE.writeTo(context, writer, suppression);
        }
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, E exception) {
        String message = exception.getMessage();
        Throwable cause = exception.getCause();
        OptionalInt size = ObjectMarshaller.INSTANCE.size(context, cause);
        if (size.isPresent()) {
            OptionalInt messageSize = AnyField.STRING.size(context, (cause == null) || !cause.toString().equals(message) ? message : null);
            StackTraceElement[] stackTrace = exception.getStackTrace();
            int stackTraceSize = CodedOutputStream.computeUInt32SizeNoTag(stackTrace.length);
            for (StackTraceElement element : stackTrace) {
                stackTraceSize += ObjectMarshaller.INSTANCE.size(context, element.getClassName()).getAsInt();
                stackTraceSize += ObjectMarshaller.INSTANCE.size(context, element.getMethodName()).getAsInt();
                stackTraceSize += ObjectMarshaller.INSTANCE.size(context, element.getFileName()).getAsInt();
                stackTraceSize += CodedOutputStream.computeUInt32SizeNoTag(element.getLineNumber());
            }
            size = OptionalInt.of(size.getAsInt() + messageSize.getAsInt() + stackTraceSize);
            Throwable[] suppressed = exception.getSuppressed();
            size = OptionalInt.of(size.getAsInt() + CodedOutputStream.computeUInt32SizeNoTag(suppressed.length));
            for (Throwable suppression : suppressed) {
                OptionalInt suppressionSize = ObjectMarshaller.INSTANCE.size(context, suppression);
                size = size.isPresent() && suppressionSize.isPresent() ? OptionalInt.of(size.getAsInt() + suppressionSize.getAsInt()) : OptionalInt.empty();
            }
        }
        return size;
    }

    private E createException(String message, Throwable cause) throws IOException {
        try {
            if (cause != null) {
                if (message != null) {
                    if (this.messageCauseConstructor != null) {
                        return this.messageCauseConstructor.newInstance(message, cause);
                    }
                } else {
                    if (this.causeConstructor != null) {
                        return this.causeConstructor.newInstance(cause);
                    }
                }
            }
            E exception = (message != null) ? ((this.messageConstructor != null) ? this.messageConstructor.newInstance(message) : null) : ((this.emptyConstructor != null) ? this.emptyConstructor.newInstance() : null);
            if (exception == null) {
                throw new NoSuchMethodException(String.format("%s(%s)", this.exceptionClass.getName(), (message != null) ? String.class.getName() : ""));
            }
            if (cause != null) {
                exception.initCause(cause);
            }
            return exception;
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IOException(e);
        }
    }
}

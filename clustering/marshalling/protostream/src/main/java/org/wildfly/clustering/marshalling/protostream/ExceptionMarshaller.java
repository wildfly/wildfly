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
import java.util.LinkedList;
import java.util.List;

import org.infinispan.protostream.descriptors.WireType;

/**
 * Generic marshaller for a Throwable.
 * @author Paul Ferraro
 * @param <E> the target type of this marshaller
 */
public class ExceptionMarshaller<E extends Throwable> implements ProtoStreamMarshaller<E> {

    private static final int CLASS_INDEX = 1;
    private static final int MESSAGE_INDEX = 2;
    private static final int CAUSE_INDEX = 3;
    private static final int STACK_TRACE_ELEMENT_INDEX = 4;
    private static final int SUPPRESSED_INDEX = 5;

    private final Class<E> exceptionClass;

    public ExceptionMarshaller(Class<E> exceptionClass) {
        this.exceptionClass = exceptionClass;
    }

    @Override
    public Class<? extends E> getJavaClass() {
        return this.exceptionClass;
    }

    @Override
    public E readFrom(ProtoStreamReader reader) throws IOException {
        Class<E> exceptionClass = this.exceptionClass;
        String message = null;
        Throwable cause = null;
        List<StackTraceElement> stackTrace = new LinkedList<>();
        List<Throwable> suppressed = new LinkedList<>();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case CLASS_INDEX:
                    exceptionClass = reader.readObject(Class.class);
                    break;
                case MESSAGE_INDEX:
                    message = reader.readString();
                    break;
                case CAUSE_INDEX:
                    cause = (Throwable) reader.readObject(Any.class).get();
                    break;
                case STACK_TRACE_ELEMENT_INDEX:
                    stackTrace.add(reader.readObject(StackTraceElement.class));
                    break;
                case SUPPRESSED_INDEX:
                    suppressed.add((Throwable) reader.readObject(Any.class).get());
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        E exception = this.createException(exceptionClass, message, cause);
        if (!stackTrace.isEmpty()) {
            exception.setStackTrace(stackTrace.toArray(new StackTraceElement[0]));
        }
        for (Throwable e : suppressed) {
            exception.addSuppressed(e);
        }
        return exception;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, E exception) throws IOException {
        if (this.exceptionClass == Throwable.class) {
            writer.writeObject(CLASS_INDEX, exception.getClass());
        }
        String message = exception.getMessage();
        Throwable cause = exception.getCause();
        // Avoid serializing redundant message
        if ((message != null) && ((cause == null) || !cause.toString().equals(message))) {
            writer.writeString(MESSAGE_INDEX, message);
        }
        if (cause != null) {
            writer.writeObject(CAUSE_INDEX, new Any(cause));
        }
        for (StackTraceElement element : exception.getStackTrace()) {
            writer.writeObject(STACK_TRACE_ELEMENT_INDEX, element);
        }
        for (Throwable suppressed : exception.getSuppressed()) {
            writer.writeObject(SUPPRESSED_INDEX, new Any(suppressed));
        }
    }

    private E createException(Class<E> exceptionClass, String message, Throwable cause) throws IOException {
        Constructor<E> emptyConstructor = this.getConstructor(exceptionClass);
        Constructor<E> messageConstructor = this.getConstructor(exceptionClass, String.class);
        Constructor<E> causeConstructor = this.getConstructor(exceptionClass, Throwable.class);
        Constructor<E> messageCauseConstructor = this.getConstructor(exceptionClass, String.class, Throwable.class);
        try {
            if (cause != null) {
                if (message != null) {
                    if (messageCauseConstructor != null) {
                        return messageCauseConstructor.newInstance(message, cause);
                    }
                } else {
                    if (causeConstructor != null) {
                        return causeConstructor.newInstance(cause);
                    }
                }
            }
            E exception = (message != null) ? ((messageConstructor != null) ? messageConstructor.newInstance(message) : null) : ((emptyConstructor != null) ? emptyConstructor.newInstance() : null);
            if (exception == null) {
                throw new NoSuchMethodException(String.format("%s(%s)", exceptionClass.getName(), (message != null) ? String.class.getName() : ""));
            }
            if (cause != null) {
                exception.initCause(cause);
            }
            return exception;
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IOException(e);
        }
    }

    private Constructor<E> getConstructor(Class<E> exceptionClass, Class<?>... parameterTypes) {
        try {
            return exceptionClass.getConstructor(parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}

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
package org.wildfly.clustering.weld.annotated.slim;

import java.io.IOException;
import java.lang.reflect.Constructor;

import jakarta.enterprise.inject.spi.AnnotatedCallable;
import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;

import org.infinispan.protostream.descriptors.WireType;
import org.jboss.weld.annotated.slim.SlimAnnotatedType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Generic marshaller for an {@link AnnotatedParameter}.
 * @author Paul Ferraro
 */
public class AnnotatedParameterMarshaller<X, T extends SlimAnnotatedType<X>, C extends AnnotatedConstructor<X>, M extends AnnotatedMethod<X>, P extends AnnotatedParameter<X>> implements ProtoStreamMarshaller<P> {

    private static final int CONSTRUCTOR_INDEX = 1;
    private static final int METHOD_INDEX = 2;
    private static final int POSITION_INDEX = 3;

    private final Class<P> targetClass;
    private final Class<C> constructorClass;
    private final Class<M> methodClass;

    public AnnotatedParameterMarshaller(Class<P> targetClass, Class<C> constructorClass, Class<M> methodClass) {
        this.targetClass = targetClass;
        this.constructorClass = constructorClass;
        this.methodClass = methodClass;
    }

    @Override
    public Class<P> getJavaClass() {
        return this.targetClass;
    }

    @Override
    public P readFrom(ProtoStreamReader reader) throws IOException {
        AnnotatedCallable<X> callable = null;
        int position = 0;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case CONSTRUCTOR_INDEX:
                    callable = reader.readObject(this.constructorClass);
                    break;
                case METHOD_INDEX:
                    callable = reader.readObject(this.methodClass);
                    break;
                case POSITION_INDEX:
                    position = reader.readUInt32();
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return this.targetClass.cast(callable.getParameters().get(position));
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, P parameter) throws IOException {
        AnnotatedCallable<X> callable = parameter.getDeclaringCallable();
        writer.writeObject(callable.getJavaMember() instanceof Constructor ? CONSTRUCTOR_INDEX : METHOD_INDEX, callable);
        int index = parameter.getPosition();
        if (index > 0) {
            writer.writeUInt32(POSITION_INDEX, index);
        }
    }}

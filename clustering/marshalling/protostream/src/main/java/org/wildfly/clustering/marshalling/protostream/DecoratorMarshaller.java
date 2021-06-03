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
import java.lang.reflect.Field;
import java.security.PrivilegedAction;
import java.util.function.UnaryOperator;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.security.ParametricPrivilegedAction;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Marshaller for a decorator that does not provide public access to its decorated object.
 * @author Paul Ferraro
 */
public class DecoratorMarshaller<T> implements ProtoStreamMarshaller<T>, ParametricPrivilegedAction<T, T> {

    static final int DECORATED_INDEX = 1;

    private final Class<? extends T> decoratorClass;
    private final UnaryOperator<T> decorator;
    private final Field field;

    /**
     * Constructs a decorator marshaller.
     * @param decoratedClass the generalized type of the decorated object
     * @param decorator the decoration function
     * @param sample a sample object used to determine the type of the decorated object
     */
    public DecoratorMarshaller(Class<T> decoratedClass, UnaryOperator<T> decorator, T sample) {
        this.decorator = decorator;
        Class<?> decoratorClass = decorator.apply(sample).getClass();
        this.decoratorClass = decoratorClass.asSubclass(decoratedClass);
        this.field = WildFlySecurityManager.doUnchecked(new PrivilegedAction<Field>() {
            @Override
            public Field run() {
                Field field = findDecoratedField(decoratorClass, decoratedClass);
                field.setAccessible(true);
                return field;
            }
        });
    }

    static Field findDecoratedField(Class<?> decoratorClass, Class<?> decoratedClass) {
        for (Field field : decoratorClass.getDeclaredFields()) {
            if (field.getType().isAssignableFrom(decoratedClass)) {
                return field;
            }
        }
        Class<?> superClass = decoratorClass.getSuperclass();
        if (superClass == null) {
            throw new IllegalStateException();
        }
        return findDecoratedField(superClass, decoratedClass);
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return this.decoratorClass;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T readFrom(ProtoStreamReader reader) throws IOException {
        T decorated = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case DECORATED_INDEX:
                    decorated = (T) reader.readObject(Any.class).get();
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return this.decorator.apply(decorated);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, T value) throws IOException {
        T decorated = WildFlySecurityManager.doUnchecked(value, this);
        if (decorated != null) {
            writer.writeObject(DECORATED_INDEX, new Any(decorated));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T run(T value) {
        try {
            return (T) this.field.get(value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}

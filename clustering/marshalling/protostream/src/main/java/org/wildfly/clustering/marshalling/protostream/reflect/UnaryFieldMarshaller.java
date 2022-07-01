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

package org.wildfly.clustering.marshalling.protostream.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.security.PrivilegedAction;
import java.util.function.Function;

import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Generic marshaller based on a single non-public field.
 * @author Paul Ferraro
 */
public class UnaryFieldMarshaller<T, F> extends UnaryMemberMarshaller<T, Field, F> {

    public UnaryFieldMarshaller(Class<? extends T> targetClass, Class<F> fieldClass, Function<F, T> factory) {
        super(targetClass, Reflect::getValue, Reflect::findField, fieldClass, factory);
    }

    public UnaryFieldMarshaller(Class<? extends T> targetClass, Class<F> fieldClass) {
        this(targetClass, fieldClass, Reflect.getConstructor(targetClass, fieldClass));
    }

    private UnaryFieldMarshaller(Class<? extends T> targetClass, Class<F> fieldClass, Constructor<? extends T> constructor) {
        this(targetClass, fieldClass, new Function<>() {
            @Override
            public T apply(F value) {
                return WildFlySecurityManager.doUnchecked(new PrivilegedAction<>() {
                    @Override
                    public T run() {
                        try {
                            return constructor.newInstance(value);
                        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                });
            }
        });
    }
}

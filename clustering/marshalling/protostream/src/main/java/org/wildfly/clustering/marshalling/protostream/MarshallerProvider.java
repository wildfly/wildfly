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

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Provides marshallers whose {@link BaseMarshaller#getJavaClass()} defines an abstract class.
 * @author Paul Ferraro
 */
public class MarshallerProvider implements SerializationContext.MarshallerProvider {

    public static enum ClassPredicate implements Predicate<Class<?>> {
        ABSTRACT() {
            @Override
            public boolean test(Class<?> superClass) {
                return Modifier.isAbstract(superClass.getModifiers());
            }
        },
        ;
    }

    private final Predicate<Class<?>> superClassPredicate;
    private final Map<String, BaseMarshaller<?>> marshallerByName = new HashMap<>();
    private final Map<Class<?>, BaseMarshaller<?>> marshallerByType = new IdentityHashMap<>();

    public MarshallerProvider(Predicate<Class<?>> superClassPredicate, BaseMarshaller<?>... marshallers) {
        this(superClassPredicate, Arrays.asList(marshallers));
    }

    public MarshallerProvider(Predicate<Class<?>> superClassPredicate, Iterable<? extends BaseMarshaller<?>> marshallers) {
        this(superClassPredicate, marshallers.iterator());
    }

    public MarshallerProvider(Predicate<Class<?>> superClassPredicate, Stream<? extends BaseMarshaller<?>> marshallers) {
        this(superClassPredicate, marshallers.iterator());
    }

    private MarshallerProvider(Predicate<Class<?>> superClassPredicate, Iterator<? extends BaseMarshaller<?>> marshallers) {
        this.superClassPredicate = superClassPredicate;
        while (marshallers.hasNext()) {
            BaseMarshaller<?> marshaller = marshallers.next();
            this.marshallerByName.put(marshaller.getTypeName(), marshaller);
            this.marshallerByType.put(marshaller.getJavaClass(), marshaller);
        }
    }

    @Override
    public BaseMarshaller<?> getMarshaller(String typeName) {
        return this.marshallerByName.get(typeName);
    }

    @Override
    public BaseMarshaller<?> getMarshaller(Class<?> javaClass) {
        Class<?> targetClass = javaClass;
        Class<?> superClass = javaClass.getSuperclass();
        // If implementation class has no marshaller, search any superclasses within the same classloader
        ClassLoader loader = WildFlySecurityManager.getClassLoaderPrivileged(targetClass);
        while (!this.marshallerByType.containsKey(targetClass) && (superClass != null) && this.superClassPredicate.test(superClass) && WildFlySecurityManager.getClassLoaderPrivileged(superClass) == loader) {
            targetClass = superClass;
            superClass = targetClass.getSuperclass();
        }
        return this.marshallerByType.get(targetClass);
    }
}

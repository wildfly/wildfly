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

package org.wildfly.clustering.marshalling.spi.util;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Field;
import java.security.PrivilegedAction;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.Iterator;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Externalizer for an {@link EnumSet}.  Handles both regular and jumbo variants.
 * @author Paul Ferraro
 */
public class EnumSetExternalizer<E extends Enum<E>> implements Externalizer<EnumSet<E>> {

    static final Field ENUM_SET_CLASS_FIELD = WildFlySecurityManager.doUnchecked(new PrivilegedAction<Field>() {
        @Override
        public Field run() {
            for (Field field : EnumSet.class.getDeclaredFields()) {
                if (field.getType() == Class.class) {
                    field.setAccessible(true);
                    return field;
                }
            }
            throw new IllegalStateException();
        }
    });

    @Override
    public void writeObject(ObjectOutput output, EnumSet<E> set) throws IOException {
        Class<?> enumClass = this.findEnumClass(set);
        output.writeObject(enumClass);
        Object[] enumValues = enumClass.getEnumConstants();
        // Represent EnumSet as a BitSet
        BitSet values = new BitSet(enumValues.length);
        for (int i = 0; i < enumValues.length; ++i) {
            values.set(i, set.contains(enumValues[i]));
        }
        UtilExternalizerProvider.BIT_SET.writeObject(output, values);
    }

    @SuppressWarnings("unchecked")
    @Override
    public EnumSet<E> readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        Class<E> enumClass = (Class<E>) input.readObject();
        BitSet values = UtilExternalizerProvider.BIT_SET.cast(BitSet.class).readObject(input);
        EnumSet<E> set = EnumSet.noneOf(enumClass);
        Object[] enumValues = enumClass.getEnumConstants();
        for (int i = 0; i < enumValues.length; ++i) {
            if (values.get(i)) {
                set.add((E) enumValues[i]);
            }
        }
        return set;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<EnumSet<E>> getTargetClass() {
        return (Class<EnumSet<E>>) (Class<?>) EnumSet.class;
    }

    private Class<?> findEnumClass(EnumSet<E> set) {
        EnumSet<E> nonEmptySet = set.isEmpty() ? EnumSet.complementOf(set) : set;
        Iterator<E> values = nonEmptySet.iterator();
        if (values.hasNext()) {
            return values.next().getDeclaringClass();
        }
        // Java allows enums with no values - thus one could technically create an empty EnumSet for such an enum
        // While this is unlikely, we need to resort to reflection to obtain the enum type
        return WildFlySecurityManager.doUnchecked(new PrivilegedAction<Class<?>>() {
            @Override
            public Class<?> run() {
                try {
                    return (Class<?>) ENUM_SET_CLASS_FIELD.get(set);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }
}

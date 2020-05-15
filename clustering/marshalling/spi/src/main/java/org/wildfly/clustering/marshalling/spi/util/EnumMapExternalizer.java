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
import java.util.EnumMap;
import java.util.Iterator;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.DefaultExternalizer;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Externalizer for an {@link EnumMap}.
 * @author Paul Ferraro
 */
public class EnumMapExternalizer<E extends Enum<E>> implements Externalizer<EnumMap<E, Object>> {

    @Override
    public void writeObject(ObjectOutput output, EnumMap<E, Object> map) throws IOException {
        Class<?> enumClass = this.findEnumClass(map);
        output.writeObject(enumClass);
        Object[] enumValues = enumClass.getEnumConstants();
        // Represent EnumMap keys as a BitSet
        BitSet keys = new BitSet(enumValues.length);
        for (int i = 0; i < enumValues.length; ++i) {
            keys.set(i, map.containsKey(enumValues[i]));
        }
        DefaultExternalizer.BIT_SET.writeObject(output, keys);
        for (Object value : map.values()) {
            output.writeObject(value);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public EnumMap<E, Object> readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        Class<E> enumClass = (Class<E>) input.readObject();
        BitSet keys = DefaultExternalizer.BIT_SET.cast(BitSet.class).readObject(input);
        EnumMap<E, Object> map = new EnumMap<>(enumClass);
        Object[] enumValues = enumClass.getEnumConstants();
        for (int i = 0; i < enumValues.length; ++i) {
            if (keys.get(i)) {
                map.put((E) enumValues[i], input.readObject());
            }
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<EnumMap<E, Object>> getTargetClass() {
        return (Class<EnumMap<E, Object>>) (Class<?>) EnumMap.class;
    }

    private Class<?> findEnumClass(EnumMap<E, Object> map) {
        Iterator<E> values = map.keySet().iterator();
        if (values.hasNext()) {
            return values.next().getDeclaringClass();
        }
        // If EnumMap is empty, we need to resort to reflection to obtain the enum type
        return WildFlySecurityManager.doUnchecked(new PrivilegedAction<Class<?>>() {
            @Override
            public Class<?> run() {
                try {
                    Field field = EnumMap.class.getDeclaredField("keyType");
                    field.setAccessible(true);
                    return (Class<?>) field.get(map);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }
}

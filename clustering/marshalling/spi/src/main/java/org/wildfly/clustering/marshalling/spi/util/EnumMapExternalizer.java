/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Externalizer for an {@link EnumMap}.
 * @author Paul Ferraro
 */
public class EnumMapExternalizer<E extends Enum<E>> implements Externalizer<EnumMap<E, Object>> {

    static final Field ENUM_MAP_KEY_CLASS_FIELD = WildFlySecurityManager.doUnchecked(new PrivilegedAction<Field>() {
        @Override
        public Field run() {
            for (Field field : EnumMap.class.getDeclaredFields()) {
                if (field.getType() == Class.class) {
                    field.setAccessible(true);
                    return field;
                }
            }
            throw new IllegalStateException();
        }
    });

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
        UtilExternalizerProvider.BIT_SET.writeObject(output, keys);
        for (Object value : map.values()) {
            output.writeObject(value);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public EnumMap<E, Object> readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        Class<E> enumClass = (Class<E>) input.readObject();
        BitSet keys = UtilExternalizerProvider.BIT_SET.cast(BitSet.class).readObject(input);
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
                    return (Class<?>) ENUM_MAP_KEY_CLASS_FIELD.get(map);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }
}

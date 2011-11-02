/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.context;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
class Primitives {
    private static final Map<Class<?>, Class<?>> primitives = new HashMap<Class<?>, Class<?>>();

    /**
     * @see     Class#isPrimitive()
     * @see     Boolean#TYPE
     * @see     Character#TYPE
     * @see     Byte#TYPE
     * @see     Short#TYPE
     * @see     Integer#TYPE
     * @see     Long#TYPE
     * @see     Float#TYPE
     * @see     Double#TYPE
     * @see     Void#TYPE
     */
    static {
        primitives.put(Boolean.TYPE, Boolean.class);
        primitives.put(Character.TYPE, Character.class);
        primitives.put(Byte.TYPE, Byte.class);
        primitives.put(Short.TYPE, Short.class);
        primitives.put(Integer.TYPE, Integer.class);
        primitives.put(Long.TYPE, Long.class);
        primitives.put(Float.TYPE, Float.class);
        primitives.put(Double.TYPE, Double.class);
        primitives.put(Void.TYPE, Void.class);
    }

    static Class<?> normalize(Class<?> possiblePrimitive) {
        if (!possiblePrimitive.isPrimitive())
            return possiblePrimitive;
        Class<?> normalizedClass = primitives.get(possiblePrimitive);
        assert normalizedClass != null : "can't find the normal class for primitive " + possiblePrimitive;
        return normalizedClass;
    }
}

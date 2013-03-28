/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.security.manager;

import java.lang.reflect.Field;
import java.security.PrivilegedAction;

/**
 * A privileged action which gets and returns a non-public field from a class.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
// note: don't make this public.  people should generally use the reflection index for this kind of thing.
final class GetAccessibleDeclaredFieldAction implements PrivilegedAction<Field> {
    private final Class<?> clazz;
    private final String fieldName;

    /**
     * Construct a new instance.
     *
     * @param clazz the class to search
     * @param fieldName the field name to search for
     */
    public GetAccessibleDeclaredFieldAction(final Class<?> clazz, final String fieldName) {
        this.clazz = clazz;
        this.fieldName = fieldName;
    }

    public Field run() {
        final Field field;
        try {
            field = clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new NoSuchFieldError(e.getMessage());
        }
        field.setAccessible(true);
        return field;
    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.spi;

import java.lang.reflect.Field;
import java.security.PrivilegedAction;
import java.util.function.Function;

import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
public enum ConfigurationBuilderAttributesAccessor implements Function<Object, AttributeSet> {

    INSTANCE;

    @Override
    public AttributeSet apply(Object builder) {
        PrivilegedAction<AttributeSet> action = new PrivilegedAction<AttributeSet>() {
            @Override
            public AttributeSet run() {
                NoSuchFieldException exception = null;
                Class<?> targetClass = builder.getClass();
                while (targetClass != Object.class) {
                    try {
                        Field field = builder.getClass().getDeclaredField("attributes");
                        try {
                            field.setAccessible(true);
                            return (AttributeSet) field.get(builder);
                        } catch (IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        } finally {
                            field.setAccessible(false);
                        }
                    } catch (NoSuchFieldException e) {
                        if (exception == null) {
                            exception = e;
                        }
                        targetClass = targetClass.getSuperclass();
                    }
                }
                throw new IllegalStateException(exception);
            }
        };
        return WildFlySecurityManager.doUnchecked(action);
    }
}

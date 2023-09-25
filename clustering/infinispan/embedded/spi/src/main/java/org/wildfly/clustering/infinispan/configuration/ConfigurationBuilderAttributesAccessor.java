/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.configuration;

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
        PrivilegedAction<AttributeSet> action = new PrivilegedAction<>() {
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

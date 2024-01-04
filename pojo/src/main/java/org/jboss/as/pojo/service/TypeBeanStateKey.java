/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.service;

import org.jboss.as.pojo.BeanState;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class TypeBeanStateKey {
    private final Class<?> type;
    private final BeanState state;

    public TypeBeanStateKey(Class<?> type, BeanState state) {
        this.type = type;
        this.state = state;
    }

    @Override
    public int hashCode() {
        return type.hashCode() + 7 * state.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TypeBeanStateKey == false)
            return false;

        TypeBeanStateKey tbsk = (TypeBeanStateKey) obj;
        return type.equals(tbsk.type) && state == tbsk.state;
    }
}

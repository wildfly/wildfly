/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.descriptor;

import org.jboss.as.pojo.logging.PojoLogger;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Value meta data.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class ValueConfig extends AbstractConfigVisitorNode implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type;
    private int index = -1;

    /**
     * Get value.
     *
     * @param type the type
     * @return value
     */
    public Object getValue(Type type) {
        if (type == null || (type instanceof Class)) {
            return getClassValue((Class) type);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            return getPtValue(pt);
        } else {
            throw PojoLogger.ROOT_LOGGER.unknownType(type);
        }
    }

    /**
     * Get value.
     *
     * @param type the parameterized type
     * @return value
     */
    protected Object getPtValue(ParameterizedType type) {
        return getValue(type.getRawType());
    }

    /**
     * Get value, use type to narrow down exact value.
     *
     * @param type the injection point type
     * @return value
     */
    protected abstract Object getClassValue(Class<?> type);

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.descriptor;

import org.jboss.as.pojo.logging.PojoLogger;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Collection meta data.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class CollectionConfig extends ValueConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    /** The element type */
    protected String elementType;
    private List<ValueConfig> values = new ArrayList<ValueConfig>();
    private Class<?> collectionType;
    private Class<?> componentType;

    protected abstract Collection<Object> createDefaultInstance();

    @SuppressWarnings({"unchecked"})
    protected Collection<Object> createInstance() {
        try {
            if (collectionType != null) {
                return (Collection<Object>) collectionType.newInstance();
            } else {
                return createDefaultInstance();
            }
        } catch (Exception e) {
            throw PojoLogger.ROOT_LOGGER.cannotInstantiateCollection(e);
        }
    }

    @Override
    public void visit(ConfigVisitor visitor) {
        collectionType = getType(visitor, getType());
        componentType = getType(visitor, elementType);
        super.visit(visitor);
    }

    @Override
    protected void addChildren(ConfigVisitor visitor, List<ConfigVisitorNode> nodes) {
        nodes.addAll(values);
    }

    protected Object getPtValue(ParameterizedType type) {
        Type ct = componentType;
        if (ct == null && type != null)
            ct = getComponentType(type, 0);

        Collection<Object> result = createInstance();
        for (ValueConfig vc : values) {
            result.add(vc.getValue(ct));
        }
        return result;
    }

    protected Object getClassValue(Class<?> type) {
        Collection<Object> result = createInstance();
        for (ValueConfig vc : values) {
            result.add(vc.getValue(componentType));
        }
        return result;
    }

    public void setElementType(String elementType) {
        this.elementType = elementType;
    }

    public void addValue(ValueConfig value) {
        values.add(value);
    }
}
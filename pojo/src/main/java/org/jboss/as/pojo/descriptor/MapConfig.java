/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.descriptor;

import org.jboss.as.pojo.logging.PojoLogger;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Map meta data.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class MapConfig extends ValueConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String keyType;
    protected String valueType;

    private Map<ValueConfig, ValueConfig> map = new HashMap<ValueConfig, ValueConfig>();

    private Class<?> mapType;
    private Class<?> keyClass;
    private Class<?> valueClass;

    @SuppressWarnings({"unchecked"})
    protected Map<Object, Object> createInstance() {
        try {
            if (mapType != null) {
                return (Map<Object, Object>) mapType.newInstance();
            } else {
                return new HashMap();
            }
        } catch (Exception e) {
            throw PojoLogger.ROOT_LOGGER.cannotInstantiateMap(e);
        }
    }

    @Override
    public void visit(ConfigVisitor visitor) {
        mapType = getType(visitor, getType());
        keyClass = getType(visitor, keyType);
        valueClass = getType(visitor, valueType);
        super.visit(visitor);
    }

    @Override
    protected void addChildren(ConfigVisitor visitor, List<ConfigVisitorNode> nodes) {
        nodes.addAll(map.keySet());
        nodes.addAll(map.values());
    }

    protected Object getPtValue(ParameterizedType type) {
        Type kt = keyClass;
        if (kt == null && type != null)
            kt = getComponentType(type, 0);
        Type vt = valueClass;
        if (vt == null && type != null)
            vt = getComponentType(type, 1);

        Map<Object, Object> result = createInstance();
        for (Map.Entry<ValueConfig, ValueConfig> entry : map.entrySet()) {
            result.put(entry.getKey().getValue(kt), entry.getValue().getValue(vt));
        }
        return result;
    }

    protected Object getClassValue(Class<?> type) {
        Map<Object, Object> result = createInstance();
        for (Map.Entry<ValueConfig, ValueConfig> entry : map.entrySet()) {
            result.put(entry.getKey().getValue(keyClass), entry.getValue().getValue(valueClass));
        }
        return result;
    }

    public void put(ValueConfig key, ValueConfig value) {
        map.put(key, value);
    }

    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }
}
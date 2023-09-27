/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.descriptor;

import org.jboss.as.pojo.logging.PojoLogger;
import org.jboss.as.pojo.service.BeanInfo;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Property meta data.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class PropertyConfig extends AbstractConfigVisitorNode implements Serializable {
    private static final long serialVersionUID = 1L;

    private String propertyName;
    private String type;
    private ValueConfig value;
    private transient BeanInfo beanInfo;

    public void visit(ConfigVisitor visitor) {
        if (value == null)
            throw PojoLogger.ROOT_LOGGER.nullValue();
        this.beanInfo = visitor.getBeanInfo();
        super.visit(visitor);
    }

    @Override
    protected void addChildren(ConfigVisitor visitor, List<ConfigVisitorNode> nodes) {
        nodes.add(value);
    }

    @Override
    public Class<?> getType(ConfigVisitor visitor, ConfigVisitorNode previous) {
        Class<?> clazz = getType(visitor, type);
        if (clazz == null) {
            Method m = beanInfo.getGetter(propertyName, null);
            return m.getReturnType();
        }
        return clazz;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ValueConfig getValue() {
        return value;
    }

    public void setValue(ValueConfig value) {
        this.value = value;
    }
}
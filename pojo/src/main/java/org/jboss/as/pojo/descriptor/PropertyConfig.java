/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
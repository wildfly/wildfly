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
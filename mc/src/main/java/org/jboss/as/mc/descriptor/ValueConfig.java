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

package org.jboss.as.mc.descriptor;

import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;

import java.io.Serializable;

/**
 * Value meta data.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ValueConfig implements Serializable, ConfigVisitorNode, Value<Object> {
    private static final long serialVersionUID = 1L;

    private String type;
    protected ConversionValue rawValue;
    private InjectedValue<Object> value = new InjectedValue<Object>();

    @Override
    public Object getValue() throws IllegalStateException, IllegalArgumentException {
        return value.getValue();
    }

    @Override
    public void visit(ConfigVisitor visitor) {
        if (type != null && rawValue != null) {
            try {
                Class<?> clazz = visitor.getClassLoader().loadClass(type);
                rawValue.setType(clazz);
            } catch (Throwable t) {
                throw new IllegalArgumentException(t);
            }
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public InjectedValue<Object> getInjectedValue() {
        return value;
    }

    public void setValue(Object value) {
        rawValue = new ConversionValue(value);
        this.value.setValue(rawValue);
    }
}
/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import org.jboss.as.mc.service.Configurator;
import org.jboss.msc.value.Value;

/**
 * Try converting value.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ConversionValue implements Value<Object> {
    private final Object value;
    private Class<?> type;
    private boolean replaceProperties = true;
    private boolean trim = true;

    public ConversionValue(Object value) {
        this.value = value;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public void setReplaceProperties(boolean replaceProperties) {
        this.replaceProperties = replaceProperties;
    }

    public void setTrim(boolean trim) {
        this.trim = trim;
    }

    @Override
    public Object getValue() throws IllegalStateException, IllegalArgumentException {
        try {
            return Configurator.convertValue(type, value, replaceProperties, trim);
        } catch (Throwable t) {
            throw new IllegalArgumentException(t);
        }
    }
}

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

package org.jboss.as.pojo.descriptor;

import org.jboss.as.pojo.logging.PojoLogger;
import org.jboss.as.pojo.service.Configurator;

/**
 * String value.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class StringValueConfig extends ValueConfig {
    private static final long serialVersionUID = 1L;

    private String value;
    private boolean replaceProperties;
    private boolean trim;
    private Class<?> clazz;

    @Override
    public void visit(ConfigVisitor visitor) {
        if (getType() != null) {
            try {
                clazz = visitor.getModule().getClassLoader().loadClass(getType());
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    @Override
    protected Object getClassValue(Class<?> type) {
        if (type == null)
            type = clazz;
        if (type == null)
            throw PojoLogger.ROOT_LOGGER.cannotDetermineInjectedType(toString());

        try {
            return Configurator.convertValue(type, value, replaceProperties, trim);
        } catch (Throwable t) {
            throw new IllegalArgumentException(t);
        }
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setReplaceProperties(boolean replaceProperties) {
        this.replaceProperties = replaceProperties;
    }

    public void setTrim(boolean trim) {
        this.trim = trim;
    }
}

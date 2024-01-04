/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.service;

import org.jboss.as.pojo.logging.PojoLogger;
import org.jboss.as.pojo.descriptor.ValueConfig;

import java.lang.reflect.Type;

/**
 * Abstract joinpoint; keep parameters.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class AbstractJoinpoint implements Joinpoint {
    private ValueConfig[] parameters;

    protected Object[] toObjects(Type[] types) {
        if (parameters == null || parameters.length == 0)
            return new Object[0];

        if (types == null || types.length != parameters.length)
            throw PojoLogger.ROOT_LOGGER.wrongTypeSize();

        try {
            Object[] result = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i] != null)
                    result[i] = Configurator.convertValue(Configurator.toClass(types[i]), parameters[i].getValue(types[i]), true, true);
            }

            return result;
        } catch (Throwable t) {
            throw new IllegalArgumentException(t);
        }
    }

    protected ValueConfig[] getParameters() {
        return parameters;
    }

    public void setParameters(ValueConfig[] parameters) {
        this.parameters = parameters;
    }
}

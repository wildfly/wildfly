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

package org.jboss.as.mc.service;

import org.jboss.as.mc.descriptor.ValueConfig;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;

/**
 * Abstract joinpoint; keep parameters.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class AbstractJoinpoint implements Joinpoint {
    private ValueConfig[] parameters;

    protected Object[] toObjects(Class[] types) {
        if (parameters == null || parameters.length == 0)
            return new Object[0];

        if (types == null || types.length != parameters.length)
            throw new IllegalArgumentException("Wrong types size, doesn't match parameters!");

        try {
            Object[] result = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i] != null)
                    result[i] = Configurator.convertValue(types[i], parameters[i].getValue(types[i]), true, true);
            }

            return result;
        } catch (Throwable t) {
            throw new IllegalArgumentException(t);
        }
    }

    public void setParameters(ValueConfig[] parameters) {
        this.parameters = parameters;
    }
}

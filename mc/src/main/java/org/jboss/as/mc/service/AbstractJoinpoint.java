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

import org.jboss.msc.value.InjectedValue;

/**
 * Abstract joinpoint; keep parameters.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class AbstractJoinpoint implements Joinpoint {
    private InjectedValue<Object>[] parameters;

    protected Object[] toObjects() {
        if (parameters == null || parameters.length == 0)
            return new Object[0];

        Object[] result = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++)
            result[i] = parameters[i].getValue();

        return result;
    }

    public InjectedValue<Object>[] getParameters() {
        return parameters;
    }

    public void setParameters(InjectedValue<Object>[] parameters) {
        this.parameters = parameters;
    }
}

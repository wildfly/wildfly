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

import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;

import java.lang.reflect.Method;

/**
 * Reflection joinpoint.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ReflectionJoinpoint extends TargetJoinpoint {
    private final DeploymentReflectionIndex index;
    private final String methodName;
    private final String[] types;

    public ReflectionJoinpoint(DeploymentReflectionIndex index, String methodName, String[] types) {
        this.index = index;
        this.methodName = methodName;
        this.types = types;
    }

    @Override
    public Object dispatch() throws Throwable {
        Object target = getTarget().getValue();
        Method method = Configurator.findMethod(index, target.getClass(), methodName, types, false, true, true);
        return method.invoke(target, toObjects(method.getParameterTypes()));
    }
}

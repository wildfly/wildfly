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

package org.jboss.as.pojo.service;

import org.jboss.as.pojo.descriptor.LifecycleConfig;
import org.jboss.as.pojo.descriptor.ValueConfig;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;

import java.lang.reflect.Method;

/**
 * POJO lifecycle phase.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class LifecyclePojoPhase extends AbstractPojoPhase {
    protected abstract LifecycleConfig getUpConfig();
    protected abstract LifecycleConfig getDownConfig();
    protected abstract String defaultUp();
    protected abstract String defaultDown();

    protected Joinpoint createJoinpoint(LifecycleConfig config, String defaultMethod) {
        Method method;
        ValueConfig[] params = null;
        if (config == null) {
            try {
                method = getBeanInfo().getMethod(defaultMethod);
            } catch (Exception t) {
                if (log.isTraceEnabled())
                    log.trace("Ignoring default " + defaultUp() + " invocation.", t);
                return null;
            }
        } else {
            String methodName = config.getMethodName();
            if (methodName == null) {
                methodName = defaultMethod;
            }
            ValueConfig[] parameters = config.getParameters();
            String[] types = Configurator.getTypes(parameters);
            method = getBeanInfo().findMethod(methodName, types);
            params = parameters;
        }
        MethodJoinpoint joinpoint = new MethodJoinpoint(method);
        joinpoint.setTarget(new ImmediateValue<Object>(getBean()));
        joinpoint.setParameters(params);
        return joinpoint;
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            Joinpoint joinpoint = createJoinpoint(getUpConfig(), defaultUp());
            if (joinpoint != null)
                joinpoint.dispatch();
        } catch (Throwable t) {
            throw new StartException(t);
        }
        super.start(context);
    }

    @Override
    public void stop(StopContext context) {
        super.stop(context);
        try {
            Joinpoint joinpoint = createJoinpoint(getDownConfig(), defaultDown());
            if (joinpoint != null)
                joinpoint.dispatch();
        } catch (Throwable t) {
            log.debug("Exception at " + defaultDown() + " phase.", t);
        }
    }
}

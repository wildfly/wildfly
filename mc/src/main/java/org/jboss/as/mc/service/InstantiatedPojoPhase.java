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

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * MC pojo instantiated phase.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class InstantiatedPojoPhase implements Service<Object> {
    private Object bean;

    private InjectedValue<InstantiationAction> instantiationAction = new InjectedValue<InstantiationAction>();
    private InjectedValue<Object>[] parameters; // dynamic size
    private InjectedValue<BeanInfo> beanInfo = new InjectedValue<BeanInfo>();

    public void start(StartContext context) throws StartException {
        Object[] params;
        if (parameters == null || parameters.length == 0) {
            params = new Object[0];
        } else {
            params = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++)
                params[i] = parameters[i].getValue();
        }
        bean = instantiationAction.getValue().instantiate(params);

        // TODO -- <install>

        final ServiceTarget serviceTarget = context.getChildTarget();
        // TODO
    }

    public void stop(StopContext context) {
    }

    public Object getValue() throws IllegalStateException, IllegalArgumentException {
        return bean;
    }

    public void setParameters(InjectedValue<Object>[] parameters) {
        this.parameters = parameters;
    }

    public InjectedValue<InstantiationAction> getInstantiationAction() {
        return instantiationAction;
    }

    public InjectedValue<BeanInfo> getBeanInfo() {
        return beanInfo;
    }
}

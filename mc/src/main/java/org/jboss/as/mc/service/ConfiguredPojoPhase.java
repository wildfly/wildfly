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

import org.jboss.as.mc.BeanState;
import org.jboss.as.mc.descriptor.PropertyConfig;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * MC pojo configured phase.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ConfiguredPojoPhase extends AbstractPojoPhase {
    @Override
    protected BeanState getLifecycleState() {
        return BeanState.CONFIGURED;
    }

    @Override
    protected AbstractPojoPhase createNextPhase() {
        return new CreateDestroyPojoPhase();
    }

    protected void configure(boolean nullify) throws Throwable {
        Set<PropertyConfig> properties = getBeanConfig().getValue().getProperties();
        if (properties != null) {
            BeanInfo beanInfo = getBeanInfo().getValue();
            for (PropertyConfig pc : properties) {
                Method setter = beanInfo.getSetter(pc.getPropertyName()); // TODO -- multi-setters
                MethodJoinpoint joinpoint = new MethodJoinpoint(setter);
                InjectedValue<Object> param = (nullify == false) ? pc.getValue().getValue() : new InjectedValue<Object>();
                joinpoint.setParameters(new InjectedValue[]{param});
                joinpoint.setTarget(getBean());
                joinpoint.dispatch();
            }
        }
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            configure(false);
        } catch (Throwable t) {
            throw new StartException(t);
        }
        super.start(context);
    }

    @Override
    public void stop(StopContext context) {
        super.stop(context);
        try {
            configure(true);
        } catch (Throwable ignored) {
        }
    }
}

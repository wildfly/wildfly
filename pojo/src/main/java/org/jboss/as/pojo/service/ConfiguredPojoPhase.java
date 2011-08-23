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

import org.jboss.as.pojo.BeanState;
import org.jboss.as.pojo.descriptor.PropertyConfig;
import org.jboss.as.pojo.descriptor.ValueConfig;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * POJO configured phase.
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
        Set<PropertyConfig> properties = getBeanConfig().getProperties();
        if (properties != null) {
            List<PropertyConfig> used = new ArrayList<PropertyConfig>();
            for (PropertyConfig pc : properties) {
                try {
                    configure(pc, nullify);
                    used.add(pc);
                } catch (Throwable t) {
                    if (nullify == false) {
                        for (PropertyConfig upc : used) {
                            try {
                                configure(upc, true);
                            } catch (Throwable ignored) {
                            }
                        }
                        throw new StartException(t);
                    }
                }
            }
        }
    }

    protected void configure(PropertyConfig pc, boolean nullify) throws Throwable {
        ValueConfig value = pc.getValue();
        Class<?> clazz = null;
        String type = pc.getType(); // check property
        if (type == null)
            type = value.getType(); // check value
        if (type != null)
            clazz = getModule().getClassLoader().loadClass(type);

        Method setter = getBeanInfo().getSetter(pc.getPropertyName(), clazz);
        MethodJoinpoint joinpoint = new MethodJoinpoint(setter);
        ValueConfig param = (nullify == false) ? value : null;
        joinpoint.setParameters(new ValueConfig[]{param});
        joinpoint.setTarget(new ImmediateValue<Object>(getBean()));
        joinpoint.dispatch();
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            configure(false);
        } catch (StartException t) {
            throw t;
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

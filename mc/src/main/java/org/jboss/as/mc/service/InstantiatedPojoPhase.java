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
import org.jboss.as.mc.descriptor.ConstructorConfig;
import org.jboss.as.mc.descriptor.ValueConfig;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.value.ImmediateValue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * MC pojo instantiated phase.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class InstantiatedPojoPhase extends AbstractPojoPhase {
    private final DeploymentReflectionIndex index;

    public InstantiatedPojoPhase(DeploymentReflectionIndex index) {
        this.index = index;
    }

    @Override
    protected BeanState getLifecycleState() {
        return BeanState.INSTANTIATED;
    }

    @Override
    protected AbstractPojoPhase createNextPhase() {
        return new ConfiguredPojoPhase();
    }

    public void start(StartContext context) throws StartException {
        try {
            Joinpoint instantiateJoinpoint;
            ConstructorConfig ctorConfig = getBeanConfig().getConstructor();
            if (ctorConfig != null) {
                String factoryMethod = ctorConfig.getFactoryMethod();
                if (factoryMethod == null)
                    throw new StartException("Missing factory method in ctor configuration: " + getBeanConfig());

                ValueConfig[] parameters = ctorConfig.getParameters();
                String[] types = Configurator.getTypes(parameters);

                String factoryClass = ctorConfig.getFactoryClass();
                if (factoryClass != null) {
                    Class<?> factoryClazz = Class.forName(factoryClass, false, getModule().getClassLoader());
                    Method method = Configurator.findMethodInfo(index, factoryClazz, factoryMethod, types, true, true, true);
                    MethodJoinpoint mj = new MethodJoinpoint(method);
                    mj.setTarget(new ImmediateValue<Object>(null)); // null, since this is static call
                    mj.setParameters(parameters);
                    instantiateJoinpoint = mj;
                } else {
                    ValueConfig factory = ctorConfig.getFactory();
                    if (factory == null)
                        throw new StartException("Missing factoy value: " + getBeanConfig());

                    ReflectionJoinpoint rj = new ReflectionJoinpoint(index, factoryMethod, types);
                    rj.setTarget(factory);
                    rj.setParameters(parameters);
                    instantiateJoinpoint = rj;
                }
            } else {
                Constructor ctor = getBeanInfo().getConstructor();
                instantiateJoinpoint = new ConstructorJoinpoint(ctor);
            }
            setBean(instantiateJoinpoint.dispatch());
        } catch (Throwable t) {
            throw new StartException(t);
        }
        super.start(context);
    }
}

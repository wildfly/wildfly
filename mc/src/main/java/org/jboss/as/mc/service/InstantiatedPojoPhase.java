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
    private final DescribedPojoPhase describedPojoPhase;

    public InstantiatedPojoPhase(DeploymentReflectionIndex index, DescribedPojoPhase describedPojoPhase) {
        this.index = index;
        this.describedPojoPhase = describedPojoPhase;
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
            BeanInfo beanInfo = getBeanInfo();
            Joinpoint instantiateJoinpoint = null;
            String[] types = Configurator.NO_PARAMS_TYPES;
            ConstructorConfig ctorConfig = getBeanConfig().getConstructor();
            if (ctorConfig != null) {
                ValueConfig[] parameters = ctorConfig.getParameters();
                types = Configurator.getTypes(parameters);

                String factoryClass = ctorConfig.getFactoryClass();
                ValueConfig factory = ctorConfig.getFactory();
                if (factoryClass != null || factory != null) {
                    String factoryMethod = ctorConfig.getFactoryMethod();
                    if (factoryMethod == null)
                        throw new StartException("Missing factory method in ctor configuration: " + getBeanConfig());

                    if (factoryClass != null) {
                        // static factory
                        Class<?> factoryClazz = Class.forName(factoryClass, false, getModule().getClassLoader());
                        Method method = Configurator.findMethod(index, factoryClazz, factoryMethod, types, true, true, true);
                        MethodJoinpoint mj = new MethodJoinpoint(method);
                        mj.setTarget(new ImmediateValue<Object>(null)); // null, since this is static call
                        mj.setParameters(parameters);
                        instantiateJoinpoint = mj;
                    } else if (factory != null) {
                        ReflectionJoinpoint rj = new ReflectionJoinpoint(index, factoryMethod, types);
                        rj.setTarget(factory);
                        rj.setParameters(parameters);
                        instantiateJoinpoint = rj;
                    }
                }
            }
            // plain bean's ctor
            if (instantiateJoinpoint == null) {
                if (beanInfo == null)
                    throw new StartException("Missing bean info, set bean's class attribute: " + getBeanConfig());

                Constructor ctor = (types.length == 0) ? beanInfo.getConstructor() : beanInfo.findConstructor(types);
                instantiateJoinpoint = new ConstructorJoinpoint(ctor);
            }

            setBean(instantiateJoinpoint.dispatch());
            if (beanInfo == null) {
                beanInfo = new DefaultBeanInfo(index, getBean().getClass());
                setBeanInfo(beanInfo);
                // set so describe service has its value
                describedPojoPhase.setBeanInfo(beanInfo);
            }
        } catch (StartException t) {
            throw t;
        } catch (Throwable t) {
            throw new StartException(t);
        }
        super.start(context);
    }
}

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
import org.jboss.as.pojo.descriptor.ConstructorConfig;
import org.jboss.as.pojo.descriptor.FactoryConfig;
import org.jboss.as.pojo.descriptor.ValueConfig;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.value.ImmediateValue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * POJO instantiated phase.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class InstantiatedPojoPhase extends AbstractPojoPhase {
    private final DescribedPojoPhase describedPojoPhase;

    public InstantiatedPojoPhase(DescribedPojoPhase describedPojoPhase) {
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
            ValueConfig[] parameters = new ValueConfig[0];
            String[] types = Configurator.NO_PARAMS_TYPES;
            ConstructorConfig ctorConfig = getBeanConfig().getConstructor();
            if (ctorConfig != null) {
                parameters = ctorConfig.getParameters();
                types = Configurator.getTypes(parameters);

                String factoryClass = ctorConfig.getFactoryClass();
                FactoryConfig factory = ctorConfig.getFactory();
                if (factoryClass != null || factory != null) {
                    String factoryMethod = ctorConfig.getFactoryMethod();
                    if (factoryMethod == null)
                        throw new StartException("Missing factory method in ctor configuration: " + getBeanConfig());

                    if (factoryClass != null) {
                        // static factory
                        Class<?> factoryClazz = Class.forName(factoryClass, false, getModule().getClassLoader());
                        Method method = Configurator.findMethod(getIndex(), factoryClazz, factoryMethod, types, true, true, true);
                        MethodJoinpoint mj = new MethodJoinpoint(method);
                        mj.setTarget(new ImmediateValue<Object>(null)); // null, since this is static call
                        mj.setParameters(parameters);
                        instantiateJoinpoint = mj;
                    } else if (factory != null) {
                        ReflectionJoinpoint rj = new ReflectionJoinpoint(factory.getBeanInfo(), factoryMethod, types);
                        // null type is ok, as this should be plain injection
                        rj.setTarget(new ImmediateValue<Object>(factory.getValue(null)));
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
                ConstructorJoinpoint constructorJoinpoint = new ConstructorJoinpoint(ctor);
                constructorJoinpoint.setParameters(parameters);
                instantiateJoinpoint = constructorJoinpoint;
            }

            setBean(instantiateJoinpoint.dispatch());
            if (beanInfo == null) {
                beanInfo = new DefaultBeanInfo(getIndex(), getBean().getClass());
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

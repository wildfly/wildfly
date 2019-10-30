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

import org.jboss.as.pojo.logging.PojoLogger;
import org.jboss.as.pojo.descriptor.BeanMetaDataConfig;
import org.jboss.as.pojo.descriptor.ConstructorConfig;
import org.jboss.as.pojo.descriptor.FactoryConfig;
import org.jboss.as.pojo.descriptor.LifecycleConfig;
import org.jboss.as.pojo.descriptor.PropertyConfig;
import org.jboss.as.pojo.descriptor.ValueConfig;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.modules.Module;
import org.jboss.msc.service.StartException;
import org.jboss.msc.value.ImmediateValue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Bean utils.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public final class BeanUtils {

    /**
     * Instantiate bean.
     *
     * @param beanConfig the bean metadata config, must not be null
     * @param beanInfo the bean info, can be null if enough info
     * @param index the reflection index, must not be null
     * @param module the current CL module, must not be null
     * @return new bean instance
     * @throws Throwable for any error
     */
    public static Object instantiateBean(BeanMetaDataConfig beanConfig, BeanInfo beanInfo, DeploymentReflectionIndex index, Module module) throws Throwable {
        Joinpoint instantiateJoinpoint = null;
        ValueConfig[] parameters = new ValueConfig[0];
        String[] types = Configurator.NO_PARAMS_TYPES;
        ConstructorConfig ctorConfig = beanConfig.getConstructor();
        if (ctorConfig != null) {
            parameters = ctorConfig.getParameters();
            types = Configurator.getTypes(parameters);

            String factoryClass = ctorConfig.getFactoryClass();
            FactoryConfig factory = ctorConfig.getFactory();
            if (factoryClass != null || factory != null) {
                String factoryMethod = ctorConfig.getFactoryMethod();
                if (factoryMethod == null)
                    throw PojoLogger.ROOT_LOGGER.missingFactoryMethod(beanConfig);

                if (factoryClass != null) {
                    // static factory
                    Class<?> factoryClazz = Class.forName(factoryClass, false, module.getClassLoader());
                    Method method = Configurator.findMethod(index, factoryClazz, factoryMethod, types, true, true, true);
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
                throw new StartException(PojoLogger.ROOT_LOGGER.missingBeanInfo(beanConfig));

            Constructor ctor = (types.length == 0) ? beanInfo.getConstructor() : beanInfo.findConstructor(types);
            ConstructorJoinpoint constructorJoinpoint = new ConstructorJoinpoint(ctor);
            constructorJoinpoint.setParameters(parameters);
            instantiateJoinpoint = constructorJoinpoint;
        }

        return instantiateJoinpoint.dispatch();
    }

    /**
     * Configure bean.
     *
     * @param beanConfig the bean metadata config, must not be null
     * @param beanInfo the bean info, can be null if enough info
     * @param module the current CL module, must not be null
     * @param bean the bean instance
     * @param nullify do we nullify property
     * @throws Throwable for any error
     */
    public static void configure(BeanMetaDataConfig beanConfig, BeanInfo beanInfo, Module module, Object bean, boolean nullify) throws Throwable {
        Set<PropertyConfig> properties = beanConfig.getProperties();
        if (properties != null) {
            List<PropertyConfig> used = new ArrayList<PropertyConfig>();
            for (PropertyConfig pc : properties) {
                try {
                    configure(beanInfo, module, bean, pc, nullify);
                    used.add(pc);
                } catch (Throwable t) {
                    if (nullify == false) {
                        for (PropertyConfig upc : used) {
                            try {
                                configure(beanInfo, module, bean,upc, true);
                            } catch (Throwable ignored) {
                            }
                        }
                        throw new StartException(t);
                    }
                }
            }
        }
    }

    /**
     * Dispatch lifecycle joinpoint.
     *
     * @param beanInfo the bean info
     * @param bean the bean instance
     * @param config the lifecycle config
     * @param defaultMethod the default method
     * @throws Throwable for any error
     */
    public static void dispatchLifecycleJoinpoint(BeanInfo beanInfo, Object bean, LifecycleConfig config, String defaultMethod) throws Throwable {
        if (config != null && config.isIgnored())
            return;

        Joinpoint joinpoint = createJoinpoint(beanInfo, bean, config, defaultMethod);
        if (joinpoint != null)
            joinpoint.dispatch();
    }

    private static Joinpoint createJoinpoint(BeanInfo beanInfo, Object bean, LifecycleConfig config, String defaultMethod) {
        Method method;
        ValueConfig[] params = null;
        if (config == null) {
            try {
                method = beanInfo.getMethod(defaultMethod);
            } catch (Exception t) {
                PojoLogger.ROOT_LOGGER.tracef(t, "Ignoring default %s invocation.", defaultMethod);
                return null;
            }
        } else {
            String methodName = config.getMethodName();
            if (methodName == null) {
                methodName = defaultMethod;
            }
            ValueConfig[] parameters = config.getParameters();
            String[] types = Configurator.getTypes(parameters);
            method = beanInfo.findMethod(methodName, types);
            params = parameters;
        }
        MethodJoinpoint joinpoint = new MethodJoinpoint(method);
        joinpoint.setTarget(new ImmediateValue<Object>(bean));
        joinpoint.setParameters(params);
        return joinpoint;
    }

    private static void configure(BeanInfo beanInfo, Module module, Object bean, PropertyConfig pc, boolean nullify) throws Throwable {
        ValueConfig value = pc.getValue();
        Class<?> clazz = null;
        String type = pc.getType(); // check property
        if (type == null)
            type = value.getType(); // check value
        if (type != null)
            clazz = module.getClassLoader().loadClass(type);

        Method setter = beanInfo.getSetter(pc.getPropertyName(), clazz);
        MethodJoinpoint joinpoint = new MethodJoinpoint(setter);
        ValueConfig param = (nullify == false) ? value : null;
        joinpoint.setParameters(new ValueConfig[]{param});
        joinpoint.setTarget(new ImmediateValue<Object>(bean));
        joinpoint.dispatch();
    }

}

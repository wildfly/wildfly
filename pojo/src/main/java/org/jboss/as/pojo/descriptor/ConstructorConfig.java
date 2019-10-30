/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.pojo.descriptor;

import org.jboss.as.pojo.logging.PojoLogger;
import org.jboss.as.pojo.service.BeanInfo;
import org.jboss.as.pojo.service.Configurator;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Ctor meta data.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ConstructorConfig extends AbstractConfigVisitorNode implements Serializable {
    private static final long serialVersionUID = 1L;

    private String factoryClass;
    private String factoryMethod;
    private FactoryConfig factory;
    private ValueConfig[] parameters;

    @Override
    protected void addChildren(ConfigVisitor visitor, List<ConfigVisitorNode> nodes) {
        if (factory != null)
            nodes.add(factory);
        if (parameters != null)
            nodes.addAll(Arrays.asList(parameters));
    }

    @Override
    public Class<?> getType(ConfigVisitor visitor, ConfigVisitorNode previous) {
        if (factory != null)
            throw PojoLogger.ROOT_LOGGER.tooDynamicFromFactory();
        if (previous instanceof ValueConfig == false)
            throw PojoLogger.ROOT_LOGGER.notValueConfig(previous);

        ValueConfig vc = (ValueConfig) previous;
        if (factoryClass != null) {
            if (factoryMethod == null)
                throw PojoLogger.ROOT_LOGGER.nullFactoryMethod();

            BeanInfo beanInfo = getTempBeanInfo(visitor, factoryClass);
            Method m = beanInfo.findMethod(factoryMethod, Configurator.getTypes(parameters));
            return m.getParameterTypes()[vc.getIndex()];
        } else {
            BeanInfo beanInfo = visitor.getBeanInfo();
            if (beanInfo == null)
                throw PojoLogger.ROOT_LOGGER.nullBeanInfo();
            Constructor ctor = beanInfo.findConstructor(Configurator.getTypes(parameters));
            return ctor.getParameterTypes()[vc.getIndex()];
        }
    }

    public String getFactoryClass() {
        return factoryClass;
    }

    public void setFactoryClass(String factoryClass) {
        this.factoryClass = factoryClass;
    }

    public String getFactoryMethod() {
        return factoryMethod;
    }

    public void setFactoryMethod(String factoryMethod) {
        this.factoryMethod = factoryMethod;
    }

    public FactoryConfig getFactory() {
        return factory;
    }

    public void setFactory(FactoryConfig factory) {
        this.factory = factory;
    }

    public ValueConfig[] getParameters() {
        return parameters;
    }

    public void setParameters(ValueConfig[] parameters) {
        this.parameters = parameters;
    }
}
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
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Lifecycle meta data.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class LifecycleConfig extends AbstractConfigVisitorNode implements Serializable {
    private static final long serialVersionUID = 1L;

    private String methodName;
    private ValueConfig[] parameters;
    private boolean ignored;

    @Override
    protected void addChildren(ConfigVisitor visitor, List<ConfigVisitorNode> nodes) {
        if (parameters != null)
            nodes.addAll(Arrays.asList(parameters));
    }

    @Override
    public Class<?> getType(ConfigVisitor visitor, ConfigVisitorNode previous) {
        if (previous instanceof ValueConfig == false)
            throw PojoLogger.ROOT_LOGGER.notValueConfig(previous);

        ValueConfig vc = (ValueConfig) previous;
        BeanInfo beanInfo = visitor.getBeanInfo();
        Method m = beanInfo.findMethod(methodName, Configurator.getTypes(parameters));
        return m.getParameterTypes()[vc.getIndex()];
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public ValueConfig[] getParameters() {
        return parameters;
    }

    public void setParameters(ValueConfig[] parameters) {
        this.parameters = parameters;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }
}
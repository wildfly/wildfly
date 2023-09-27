/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
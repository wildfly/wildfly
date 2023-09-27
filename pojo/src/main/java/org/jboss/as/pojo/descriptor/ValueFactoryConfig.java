/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.descriptor;

import org.jboss.as.pojo.service.ReflectionJoinpoint;

import java.util.Arrays;
import java.util.List;

/**
 * Value factory value.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ValueFactoryConfig extends FactoryConfig {
    private static final long serialVersionUID = 1L;

    private String method;
    private ValueConfig[] parameters;

    protected Object getClassValue(Class<?> type) {
        try {
            ReflectionJoinpoint joinpoint = new ReflectionJoinpoint(beanInfo.getValue(), method);
            joinpoint.setTarget(value);
            joinpoint.setParameters(parameters);
            return joinpoint.dispatch();
        } catch (Throwable t) {
            throw new IllegalArgumentException(t);
        }
    }

    @Override
    protected void addChildren(ConfigVisitor visitor, List<ConfigVisitorNode> nodes) {
        if (parameters != null) {
            nodes.addAll(Arrays.asList(parameters));
        }
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setParameters(ValueConfig[] parameters) {
        this.parameters = parameters;
    }
}

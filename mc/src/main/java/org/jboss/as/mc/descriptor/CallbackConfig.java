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

package org.jboss.as.mc.descriptor;

import org.jboss.as.mc.BeanState;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.InjectedValue;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

/**
 * Callback meta data.
 * Atm this is simplified version of what we had in JBossAS5/6.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CallbackConfig extends AbstractConfigVisitorNode implements Serializable {
    private static final long serialVersionUID = 1L;

    private String methodName;
    private BeanState whenRequired = BeanState.INSTALLED;
    private BeanState state = BeanState.INSTALLED;
    private String signature;

    private final InjectedValue<Set<Object>> beans = new InjectedValue<Set<Object>>();

    @Override
    public void visit(ConfigVisitor visitor) {
        if (visitor.getState().next() == whenRequired) {
            Method m = visitor.getBeanInfo().findMethod(methodName, signature);
            if (m.getParameterTypes().length != 1)
                throw new IllegalArgumentException("Illegal method parameter length: " + m);
            ServiceName dependency = BeanMetaDataConfig.toInstancesName(m.getParameterTypes()[0], state);
            visitor.addOptionalDependency(dependency, beans);
        }
    }

    public Set<Object> getBeans() {
        Set<Object> set = beans.getOptionalValue();
        return (set != null) ? set : Collections.emptySet();
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public BeanState getWhenRequired() {
        return whenRequired;
    }

    public void setWhenRequired(BeanState whenRequired) {
        this.whenRequired = whenRequired;
    }

    public BeanState getState() {
        return state;
    }

    public void setState(BeanState state) {
        this.state = state;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
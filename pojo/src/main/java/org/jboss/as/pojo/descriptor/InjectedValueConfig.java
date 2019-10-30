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

package org.jboss.as.pojo.descriptor;

import org.jboss.as.pojo.BeanState;
import org.jboss.as.pojo.logging.PojoLogger;
import org.jboss.as.pojo.service.BeanInfo;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.InjectedValue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Injected value.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class InjectedValueConfig extends ValueConfig {
    private static final long serialVersionUID = 1L;

    private String bean;
    private BeanState state;
    private String service;
    private String property;

    private final transient InjectedValue<BeanInfo> beanInfo = new InjectedValue<BeanInfo>();
    private final transient InjectedValue<Object> value = new InjectedValue<Object>();

    protected Object getClassValue(Class<?> type) {
        Object result = value.getValue();
        if (result instanceof Set) {
            Set set = (Set) result;
            if (set.size() != 1)
                throw PojoLogger.ROOT_LOGGER.invalidMatchSize(set, type);
            result = set.iterator().next();
        }
        if (property != null) {
            Method getter = getBeanInfo(result).getGetter(property, type);
            try {
                return getter.invoke(result);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            } catch (InvocationTargetException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            return result;
        }
    }

    @SuppressWarnings({"unchecked"})
    protected BeanInfo getBeanInfo(Object bean) {
        BeanInfo bi = beanInfo.getOptionalValue();
        if (bi == null) {
            bi = getTempBeanInfo(bean.getClass());
        }
        return bi;
    }

    @Override
    public void visit(ConfigVisitor visitor) {
        if (bean != null) {
            visitor.addDependency(bean, BeanState.DESCRIBED, beanInfo);
            ServiceName name = BeanMetaDataConfig.toBeanName(bean, state);
            visitor.addDependency(name, value); // direct name, since we have describe already
        } else if (service != null) {
            visitor.addDependency(ServiceName.parse(service), value);
        } else {
            Class<?> type = getType(visitor, getType());
            if (type == null)
                type = getType(visitor, this);
            if (type == null)
                throw PojoLogger.ROOT_LOGGER.cannotDetermineInjectedType(toString());

            ServiceName instancesName = BeanMetaDataConfig.toInstancesName(type, state);
            visitor.addDependency(instancesName, value);
        }
    }

    public void setBean(String dependency) {
        this.bean = dependency;
    }

    public void setState(BeanState state) {
        this.state = state;
    }

    public void setService(String service) {
        this.service = service;
    }

    public void setProperty(String property) {
        this.property = property;
    }
}

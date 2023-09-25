/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

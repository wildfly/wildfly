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

/**
 * Factory value.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class FactoryConfig extends ValueConfig {
    private static final long serialVersionUID = 1L;

    private String bean;
    private BeanState state;

    protected final transient InjectedValue<BeanInfo> beanInfo = new InjectedValue<BeanInfo>();
    protected final transient InjectedValue<Object> value = new InjectedValue<Object>();

    protected Object getClassValue(Class<?> type) {
        return value.getValue();
    }

    @Override
    public void visit(ConfigVisitor visitor) {
        if (bean != null) {
            visitor.addDependency(bean, BeanState.DESCRIBED, beanInfo);
            ServiceName name = BeanMetaDataConfig.toBeanName(bean, state);
            visitor.addDependency(name, value); // direct name, since we have describe already
        }
        super.visit(visitor);
    }

    public Class<?> getType(ConfigVisitor visitor, ConfigVisitorNode previous) {
        throw PojoLogger.ROOT_LOGGER.tooDynamicFromFactory();
    }

    public void setBean(String dependency) {
        this.bean = dependency;
    }

    public void setState(BeanState state) {
        this.state = state;
    }

    public BeanInfo getBeanInfo() {
        return beanInfo.getValue();
    }
}

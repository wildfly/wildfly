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
 * Install meta data.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class InstallConfig extends LifecycleConfig {
    private static final long serialVersionUID = 1L;

    private final transient InjectedValue<BeanInfo> beanInfo = new InjectedValue<BeanInfo>();
    private final transient InjectedValue<Object> bean = new InjectedValue<Object>();

    private String dependency;
    private BeanState whenRequired = BeanState.INSTALLED;
    private BeanState dependencyState;

    @Override
    public void visit(ConfigVisitor visitor) {
        if (visitor.getState().next() == whenRequired) {
            if (dependency != null) {
                visitor.addDependency(dependency, BeanState.DESCRIBED, getBeanInfo());
                ServiceName name = BeanMetaDataConfig.toBeanName(dependency, dependencyState);
                visitor.addDependency(name, getBean()); // direct name, since we have describe already
            }
            super.visit(visitor);
        }
    }

    @Override
    public Class<?> getType(ConfigVisitor visitor, ConfigVisitorNode previous) {
        if (dependency != null)
            throw PojoLogger.ROOT_LOGGER.tooDynamicFromDependency();

        return super.getType(visitor, previous);
    }

    public String getDependency() {
        return dependency;
    }

    public void setDependency(String dependency) {
        this.dependency = dependency;
    }

    public BeanState getWhenRequired() {
        return whenRequired;
    }

    public void setWhenRequired(BeanState whenRequired) {
        this.whenRequired = whenRequired;
    }

    public void setDependencyState(BeanState dependencyState) {
        this.dependencyState = dependencyState;
    }

    public InjectedValue<BeanInfo> getBeanInfo() {
        return beanInfo;
    }

    public InjectedValue<Object> getBean() {
        return bean;
    }
}
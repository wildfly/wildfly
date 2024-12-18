/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.descriptor;

import org.jboss.as.pojo.BeanState;
import org.jboss.as.pojo.service.BeanInfo;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.modules.Module;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * Default config visitor.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author <a href="mailto:ropalka@jboss.org">Richard Opalka</a>
 */
public class DefaultConfigVisitor extends AbstractConfigVisitor {
    private final ServiceBuilder builder;
    private final BeanState state;
    private final Module module;
    private final DeploymentReflectionIndex index;
    private final BeanInfo beanInfo;

    public DefaultConfigVisitor(ServiceBuilder builder, BeanState state, Module module, DeploymentReflectionIndex index) {
        this(builder, state, module, index, null);
    }

    public DefaultConfigVisitor(ServiceBuilder builder, BeanState state, Module module, DeploymentReflectionIndex index, BeanInfo beanInfo) {
        this.builder = builder;
        this.state = state;
        this.module = module;
        this.index = index;
        this.beanInfo = beanInfo;
    }

    @Override
    public BeanState getState() {
        return state;
    }

    @Override
    public Module getModule() {
        return module;
    }

    @Override
    public Module loadModule(String identifier) {
        try {
            return module.getModule(identifier);
        } catch (Throwable t) {
            throw new IllegalArgumentException(t);
        }
    }

    @Override
    public DeploymentReflectionIndex getReflectionIndex() {
        return index;
    }

    @Override
    public BeanInfo getBeanInfo() {
        return beanInfo;
    }

    @Override
    public void addDependency(ServiceName name) {
        builder.requires(name);
    }

    @Override
    public void addDependency(ServiceName name, Injector injector) {
        builder.addDependency(name, Object.class, injector);
    }

    @Override
    public void addDependency(String bean, BeanState state) {
        if (state != BeanState.DESCRIBED)
            builder.requires(BeanMetaDataConfig.toBeanName(bean, BeanState.DESCRIBED));
        builder.requires(BeanMetaDataConfig.toBeanName(bean, state));
    }

    @Override
    public void addDependency(String bean, BeanState state, Injector injector) {
        if (state != BeanState.DESCRIBED)
            builder.requires(BeanMetaDataConfig.toBeanName(bean, BeanState.DESCRIBED));
        builder.addDependency(BeanMetaDataConfig.toBeanName(bean, state), Object.class, injector);
    }
}

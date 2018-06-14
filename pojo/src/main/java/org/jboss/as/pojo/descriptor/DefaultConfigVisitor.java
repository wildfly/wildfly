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
import org.jboss.as.pojo.service.BeanInfo;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
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
    public Module loadModule(ModuleIdentifier identifier) {
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
        builder.addDependency(name);
    }

    @Override
    public void addDependency(ServiceName name, Injector injector) {
        builder.addDependency(name, injector);
    }

    @Override
    public void addDependency(String bean, BeanState state) {
        if (state != BeanState.DESCRIBED)
            builder.addDependency(BeanMetaDataConfig.toBeanName(bean, BeanState.DESCRIBED));
        builder.addDependency(BeanMetaDataConfig.toBeanName(bean, state));
    }

    @Override
    public void addDependency(String bean, BeanState state, Injector injector) {
        if (state != BeanState.DESCRIBED)
            builder.addDependency(BeanMetaDataConfig.toBeanName(bean, BeanState.DESCRIBED));
        builder.addDependency(BeanMetaDataConfig.toBeanName(bean, state), injector);
    }
}

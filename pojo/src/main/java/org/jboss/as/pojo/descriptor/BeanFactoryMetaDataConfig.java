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

import org.jboss.as.pojo.BeanState;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * The legacy bean factory meta data.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class BeanFactoryMetaDataConfig extends BeanMetaDataConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    private final BeanMetaDataConfig bean;

    public BeanFactoryMetaDataConfig(BeanMetaDataConfig bean) {
        this.bean = bean;

        setName(bean.getName());
        bean.setName(getName() + "_Bean");
        setBeanClass(BaseBeanFactory.class.getName());

        ModuleConfig moduleConfig = new ModuleConfig();
        moduleConfig.setModuleName("org.jboss.as.pojo");
        setModule(moduleConfig);

        PropertyConfig pc = new PropertyConfig();
        pc.setPropertyName("bmd");
        ValueConfig vc = new ValueConfig() {
            protected Object getClassValue(Class<?> type) {
                return BeanFactoryMetaDataConfig.this.bean;
            }
        };
        pc.setValue(vc);
        setProperties(Collections.singleton(pc));

        LifecycleConfig ignoredLifecycle = new LifecycleConfig();
        ignoredLifecycle.setIgnored(true);
        setCreate(ignoredLifecycle);
        setStart(ignoredLifecycle);
        setStop(ignoredLifecycle);
        setDestroy(ignoredLifecycle);
    }

    @Override
    protected void addChildren(ConfigVisitor visitor, List<ConfigVisitorNode> nodes) {
        nodes.add(bean); // always check bean meta data

        BeanState state = visitor.getState();
        if (state == BeanState.NOT_INSTALLED)
            nodes.add(getModule());
        if (state == BeanState.INSTANTIATED)
            nodes.addAll(getProperties());
    }
}
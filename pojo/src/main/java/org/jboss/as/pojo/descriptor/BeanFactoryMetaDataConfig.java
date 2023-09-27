/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
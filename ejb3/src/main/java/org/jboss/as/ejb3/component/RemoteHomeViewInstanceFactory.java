/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component;

import java.util.Map;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.component.ViewInstanceFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBHomeLocator;

/**
 * @author Stuart Douglas
 */
public class RemoteHomeViewInstanceFactory implements ViewInstanceFactory {

    private final String applicationName;
    private final String moduleName;
    private final String distinctName;
    private final String beanName;

    public RemoteHomeViewInstanceFactory(final String applicationName, final String moduleName, final String distinctName, final String beanName) {
        this.applicationName = applicationName == null ? "" : applicationName;
        this.moduleName = moduleName;
        this.distinctName = distinctName;
        this.beanName = beanName;
    }

    @Override
    public ManagedReference createViewInstance(final ComponentView componentView, final Map<Object, Object> contextData) {
        Object value = EJBClient.createProxy(new EJBHomeLocator(componentView.getViewClass(), applicationName, moduleName, beanName, distinctName, Affinity.LOCAL));
        return new ValueManagedReference(value);
    }

}

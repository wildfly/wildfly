/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component.session;

import java.util.Map;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.component.ViewInstanceFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBIdentifier;
import org.jboss.ejb.client.StatelessEJBLocator;

/**
 * @author Stuart Douglas
 */
public class StatelessRemoteViewInstanceFactory implements ViewInstanceFactory {

    private final EJBIdentifier identifier;

    public StatelessRemoteViewInstanceFactory(final String applicationName, final String moduleName, final String distinctName, final String beanName) {
        this(new EJBIdentifier(applicationName == null ? "" : applicationName, moduleName, beanName, distinctName));
    }

    public StatelessRemoteViewInstanceFactory(final EJBIdentifier identifier) {
        this.identifier = identifier;
    }

    @Override
    public ManagedReference createViewInstance(final ComponentView componentView, final Map<Object, Object> contextData) {
        Object value = EJBClient.createProxy(StatelessEJBLocator.create(componentView.getViewClass(), identifier, Affinity.LOCAL));
        return new ValueManagedReference(value);
    }

}

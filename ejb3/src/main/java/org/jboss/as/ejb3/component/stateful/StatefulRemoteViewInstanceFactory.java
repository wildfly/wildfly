/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component.stateful;

import java.util.Map;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.component.ViewInstanceFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBIdentifier;
import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.StatefulEJBLocator;
import org.jboss.ejb.client.StatelessEJBLocator;

/**
 * @author Stuart Douglas
 */
public class StatefulRemoteViewInstanceFactory implements ViewInstanceFactory {

    private final EJBIdentifier identifier;

    public StatefulRemoteViewInstanceFactory(final String applicationName, final String moduleName, final String distinctName, final String beanName) {
        this(new EJBIdentifier(applicationName == null ? "" : applicationName, moduleName, beanName, distinctName));
    }

    public StatefulRemoteViewInstanceFactory(final EJBIdentifier identifier) {
        this.identifier = identifier;
    }

    @Override
    public ManagedReference createViewInstance(final ComponentView componentView, final Map<Object, Object> contextData) throws Exception {
        SessionID sessionID = (SessionID) contextData.get(SessionID.class);
        final StatefulEJBLocator<?> statefulEJBLocator;
        final StatefulSessionComponent statefulSessionComponent = (StatefulSessionComponent) componentView.getComponent();
        if (sessionID == null) {
            statefulEJBLocator = EJBClient.createSession(StatelessEJBLocator.create(componentView.getViewClass(), identifier, Affinity.LOCAL));
        } else {
            statefulEJBLocator = StatefulEJBLocator.create(componentView.getViewClass(), identifier, sessionID, statefulSessionComponent.getCache().getStrongAffinity());
        }
        final Object ejbProxy = EJBClient.createProxy(statefulEJBLocator);
        return new ValueManagedReference(ejbProxy);
    }


}

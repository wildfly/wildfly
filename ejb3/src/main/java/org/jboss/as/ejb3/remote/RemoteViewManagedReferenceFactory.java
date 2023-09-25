/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.remote;

import java.util.function.Supplier;
import jakarta.ejb.EJBHome;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.naming.ContextListAndJndiViewManagedReferenceFactory;
import org.jboss.as.naming.JndiViewManagedReferenceFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBHomeLocator;
import org.jboss.ejb.client.EJBIdentifier;
import org.jboss.ejb.client.EJBLocator;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Managed reference factory for remote Jakarta Enterprise Beans views that are bound to java: JNDI locations
 *
 * @author Stuart Douglas
 * @author Eduardo Martins
 */
public class RemoteViewManagedReferenceFactory implements ContextListAndJndiViewManagedReferenceFactory {

    private final EJBIdentifier identifier;
    private final String viewClass;
    private final boolean stateful;
    private final Supplier<ClassLoader> viewClassLoader;
    private final boolean appclient;

    public RemoteViewManagedReferenceFactory(final String appName, final String moduleName, final String distinctName, final String beanName, final String viewClass, final boolean stateful, final Supplier<ClassLoader> viewClassLoader, boolean appclient) {
        this(new EJBIdentifier(appName == null ? "" : appName, moduleName, beanName, distinctName), viewClass, stateful, viewClassLoader, appclient);
    }

    public RemoteViewManagedReferenceFactory(final EJBIdentifier identifier, final String viewClass, final boolean stateful, final Supplier<ClassLoader> viewClassLoader, boolean appclient) {
        this.identifier = identifier;
        this.viewClass = viewClass;
        this.stateful = stateful;
        this.viewClassLoader = viewClassLoader;
        this.appclient = appclient;
    }

    @Override
    public String getInstanceClassName() {
        return viewClass;
    }

    @Override
    public String getJndiViewInstanceValue() {
        return stateful ? JndiViewManagedReferenceFactory.DEFAULT_JNDI_VIEW_INSTANCE_VALUE : String.valueOf(getReference()
                .getInstance());
    }

    @Override
    public ManagedReference getReference() {
        Class<?> viewClass;
        try {
            viewClass = Class.forName(this.viewClass, false, WildFlySecurityManager.getCurrentContextClassLoaderPrivileged());
        } catch (ClassNotFoundException e) {
            if(viewClassLoader == null || viewClassLoader.get() == null) {
                throw EjbLogger.ROOT_LOGGER.failToLoadViewClassEjb(identifier.toString(), e);
            }
            try {
                viewClass = Class.forName(this.viewClass, false, viewClassLoader.get());
            } catch (ClassNotFoundException ce) {
                throw EjbLogger.ROOT_LOGGER.failToLoadViewClassEjb(identifier.toString(), ce);
            }
        }
        EJBLocator<?> ejbLocator;
        if (EJBHome.class.isAssignableFrom(viewClass)) {
            ejbLocator = EJBHomeLocator.create(viewClass.asSubclass(EJBHome.class), identifier, appclient ? Affinity.NONE : Affinity.LOCAL);
        } else if (stateful) {
            try {
                ejbLocator = EJBClient.createSession(StatelessEJBLocator.create(viewClass, identifier, appclient ? Affinity.NONE : Affinity.LOCAL));
            } catch (Exception e) {
                throw EjbLogger.ROOT_LOGGER.failedToCreateSessionForStatefulBean(e, identifier.toString());
            }
        } else {
            ejbLocator = StatelessEJBLocator.create(viewClass, identifier, appclient ? Affinity.NONE : Affinity.LOCAL);
        }
        final Object proxy = EJBClient.createProxy(ejbLocator);

        return new ValueManagedReference(proxy);
    }
}

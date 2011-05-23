/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.web.deployment;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Realm;
import org.apache.catalina.core.StandardContext;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.as.web.NamingValve;
import org.jboss.as.web.deployment.jsf.JsfInjectionProvider;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * A service starting a web deployment.
 *
 * @author Emanuel Muckenhuber
 */
class WebDeploymentService implements Service<Context> {

    private static final Logger log = Logger.getLogger("org.jboss.web");
    private final StandardContext context;
    private final InjectedValue<NamespaceContextSelector> namespaceSelector = new InjectedValue<NamespaceContextSelector>();
    private final InjectedValue<Realm> realm = new InjectedValue<Realm>();
    private final WebInjectionContainer injectionContainer;

    public WebDeploymentService(final StandardContext context, final WebInjectionContainer injectionContainer) {
        this.context = context;
        this.injectionContainer = injectionContainer;
    }

    /** {@inheritDoc} */
    public synchronized void start(StartContext startContext) throws StartException {
        context.setRealm(realm.getValue());

        JsfInjectionProvider.getInjectionContainer().set(injectionContainer);
        try {
            NamingValve.beginComponentStart(namespaceSelector.getOptionalValue());
            try {
                try {
                    context.create();
                } catch (Exception e) {
                    throw new StartException("failed to create context", e);
                }
                try {
                    context.start();
                } catch (LifecycleException e) {
                    throw new StartException("failed to start context", e);
                }
                log.info("registering web context: " + context.getName());
            } finally {
                NamingValve.endComponentStart();
            }
        } finally {
            JsfInjectionProvider.getInjectionContainer().set(null);
        }
    }

    /** {@inheritDoc} */
    public synchronized void stop(StopContext stopContext) {
        try {
            context.stop();
        } catch (LifecycleException e) {
            log.error("exception while stopping context", e);
        }
        try {
            context.destroy();
        } catch (Exception e) {
            log.error("exception while destroying context", e);
        }
    }

    /** {@inheritDoc} */
    public synchronized Context getValue() throws IllegalStateException {
        final Context context = this.context;
        if (context == null) {
            throw new IllegalStateException();
        }
        return context;
    }

    public InjectedValue<NamespaceContextSelector> getNamespaceSelector() {
        return namespaceSelector;
    }

    public InjectedValue<Realm> getRealm() {
        return realm;
    }

}

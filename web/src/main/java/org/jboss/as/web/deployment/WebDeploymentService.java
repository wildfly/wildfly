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

import static org.jboss.as.web.WebMessages.MESSAGES;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Realm;
import org.apache.catalina.core.StandardContext;
import org.jboss.as.server.deployment.SetupAction;
import org.jboss.as.web.ThreadSetupBindingListener;
import org.jboss.as.web.WebLogger;
import org.jboss.as.web.deployment.jsf.JsfInjectionProvider;
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

    private final StandardContext context;
    private final InjectedValue<Realm> realm = new InjectedValue<Realm>();
    private final WebInjectionContainer injectionContainer;
    private final List<SetupAction> setupActions;
    final List<ServletContextAttribute> attributes;

    public WebDeploymentService(final StandardContext context, final WebInjectionContainer injectionContainer, final List<SetupAction> setupActions, final List<ServletContextAttribute> attributes) {
        this.context = context;
        this.injectionContainer = injectionContainer;
        this.setupActions = setupActions;
        this.attributes = attributes;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void start(StartContext startContext) throws StartException {
        if (attributes != null) {
            final ServletContext context = this.context.getServletContext();
            for (ServletContextAttribute attribute : attributes) {
                context.setAttribute(attribute.getName(), attribute.getValue());
            }
        }

        context.setRealm(realm.getValue());

        JsfInjectionProvider.getInjectionContainer().set(injectionContainer);
        final List<SetupAction> actions = new ArrayList<SetupAction>();
        actions.addAll(setupActions);
        context.setInstanceManager(injectionContainer);
        context.setThreadBindingListener(new ThreadSetupBindingListener(actions));
        try {
            try {
                context.create();
            } catch (Exception e) {
                throw new StartException(MESSAGES.createContextFailed(), e);
            }
            try {
                context.start();
            } catch (LifecycleException e) {
                throw new StartException(MESSAGES.startContextFailed(), e);
            }
            if (context.getState() != 1) {
                throw new StartException(MESSAGES.startContextFailed());
            }
            WebLogger.WEB_LOGGER.registerWebapp(context.getName());
        } finally {
            JsfInjectionProvider.getInjectionContainer().set(null);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void stop(StopContext stopContext) {
        try {
            context.stop();
        } catch (LifecycleException e) {
            WebLogger.WEB_LOGGER.stopContextFailed(e);
        }
        try {
            context.destroy();
        } catch (Exception e) {
            WebLogger.WEB_LOGGER.destroyContextFailed(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Context getValue() throws IllegalStateException {
        final Context context = this.context;
        if (context == null) {
            throw new IllegalStateException();
        }
        return context;
    }

    public InjectedValue<Realm> getRealm() {
        return realm;
    }

}

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
package org.jboss.as.web;

import static org.jboss.as.web.WebMessages.MESSAGES;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import javax.naming.NamingException;

import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Loader;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.ContextConfig;
import org.apache.tomcat.InstanceManager;
import org.jboss.as.server.mgmt.domain.HttpManagement;
import org.jboss.as.web.deployment.WebCtxLoader;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * A service starting a welcome web context driven by simple static content.
 *
 * @author Jason T. Greene
 * @author Scott stark (sstark@redhat.com) (C) 2011 Red Hat Inc.
 */
class WelcomeContextService implements Service<Context> {

    private final StandardContext context;
    private final InjectedValue<String> pathInjector = new InjectedValue<String>();
    private final InjectedValue<VirtualHost> hostInjector = new InjectedValue<VirtualHost>();
    private final InjectedValue<HttpManagement> httpManagementInjector = new InjectedValue<HttpManagement>();

    public WelcomeContextService() {
        this.context = new StandardContext();
    }

    /** {@inheritDoc} */
    public synchronized void start(StartContext startContext) throws StartException {
        HttpManagement httpManagement = httpManagementInjector.getOptionalValue();
            try {
                context.setPath("");
                context.addLifecycleListener(new ContextConfig());
                context.setDocBase(pathInjector.getValue() + File.separatorChar + "welcome-content");

                final Loader loader = new WebCtxLoader(this.getClass().getClassLoader());
                Host host = hostInjector.getValue().getHost();
                loader.setContainer(host);
                context.setLoader(loader);
                context.setInstanceManager(new LocalInstanceManager(httpManagement));

                context.setReplaceWelcomeFiles(true);
                if (httpManagement != null) {
                    context.addWelcomeFile("index.html");
                } else {
                    context.addWelcomeFile("index_noconsole.html");
                }

                Wrapper wrapper = context.createWrapper();
                wrapper.setName("default");
                wrapper.setServletClass("org.apache.catalina.servlets.DefaultServlet");
                context.addChild(wrapper);

                context.addServletMapping("/", "default");
                context.addMimeMapping("html", "text/html");
                context.addMimeMapping("jpg", "image/jpeg");

                // Add the WelcomeContextConsoleServlet
                WelcomeContextConsoleServlet wccs = new WelcomeContextConsoleServlet(httpManagement);
                Wrapper wccsWrapper = context.createWrapper();
                wccsWrapper.setName("WelcomeContextConsoleServlet");
                wccsWrapper.setServlet(wccs);
                wccsWrapper.setServletClass(wccs.getClass().getName());
                context.addChild(wccsWrapper);

                context.addServletMapping("/console", "WelcomeContextConsoleServlet");

                host.addChild(context);
                context.create();
            } catch (Exception e) {
                throw new StartException(MESSAGES.createWelcomeContextFailed(), e);
            }
            try {
                context.start();
            } catch (LifecycleException e) {
                throw new StartException(MESSAGES.startWelcomeContextFailed(), e);
            }
    }

    /** {@inheritDoc} */
    public synchronized void stop(StopContext stopContext) {
        try {
            hostInjector.getValue().getHost().removeChild(context);
            context.stop();
        } catch (LifecycleException e) {
            WebLogger.WEB_LOGGER.stopWelcomeContextFailed(e);
        }
        try {
            context.destroy();
        } catch (Exception e) {
            WebLogger.WEB_LOGGER.destroyWelcomeContextFailed(e);
        }
    }

    /** {@inheritDoc} */
    public synchronized Context getValue() throws IllegalStateException {
        final Context context = this.context;
        if (context == null) {
            throw MESSAGES.nullValue();
        }
        return context;
    }

    public InjectedValue<String> getPathInjector() {
        return pathInjector;
    }

    public InjectedValue<VirtualHost> getHostInjector() {
        return hostInjector;
    }

    public Injector<HttpManagement> getHttpManagementInjector() {
        return httpManagementInjector;
    }

    private static class LocalInstanceManager implements InstanceManager {
        private final HttpManagement httpManagement;
        LocalInstanceManager(HttpManagement httpManagement) {
            this.httpManagement = httpManagement;
        }
        @Override
        public Object newInstance(String className) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException, ClassNotFoundException {
            if(className.equals(WelcomeContextConsoleServlet.class.getName()) == false) {
                return Class.forName(className).newInstance();
            }
            WelcomeContextConsoleServlet wccs = new WelcomeContextConsoleServlet(httpManagement);
            return wccs;
        }

        @Override
        public Object newInstance(String fqcn, ClassLoader classLoader) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException, ClassNotFoundException {
            return Class.forName(fqcn, false, classLoader).newInstance();
        }

        @Override
        public Object newInstance(Class<?> c) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException {
            return c.newInstance();
        }

        @Override
        public void newInstance(Object o) throws IllegalAccessException, InvocationTargetException, NamingException {
            throw new IllegalStateException();
        }

        @Override
        public void destroyInstance(Object o) throws IllegalAccessException, InvocationTargetException {
        }
    }
}

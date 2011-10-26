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
package org.jboss.as.jaxr.service;

import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Loader;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.ContextConfig;
import org.apache.tomcat.InstanceManager;
import org.jboss.as.server.mgmt.HttpManagementService;
import org.jboss.as.server.mgmt.domain.HttpManagement;
import org.jboss.as.web.VirtualHost;
import org.jboss.as.web.WebSubsystemServices;
import org.jboss.as.web.deployment.WebCtxLoader;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import javax.naming.NamingException;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

/**
 * A service starting a welcome web context driven by simple static content.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 26-Oct-2011
 */
public final class JUDDIContextService implements Service<Context> {

    static final ServiceName JUDDI_CONTEXT_SERVICE_NAME = JAXRConfiguration.SERVICE_BASE_NAME.append("juddi", "context");

    // [TODO] AS7-2277 JAXR subsystem i18n
    private final Logger log = Logger.getLogger(JUDDIContextService.class);

    private final StandardContext context;
    private final InjectedValue<VirtualHost> hostInjector = new InjectedValue<VirtualHost>();
    private final InjectedValue<HttpManagement> httpManagementInjector = new InjectedValue<HttpManagement>();

    public static ServiceController<?> addService(final ServiceTarget target, final JAXRConfiguration config, final ServiceListener<Object>... listeners) {
        JUDDIContextService service = new JUDDIContextService(config);
        ServiceBuilder<?> builder = target.addService(JUDDI_CONTEXT_SERVICE_NAME, service);
        builder.addDependency(WebSubsystemServices.JBOSS_WEB_HOST.append("default-host"), VirtualHost.class, service.hostInjector);
        builder.addDependency(ServiceBuilder.DependencyType.OPTIONAL, HttpManagementService.SERVICE_NAME, HttpManagement.class, service.httpManagementInjector);
        builder.addListener(listeners);
        return builder.install();
    }

    private JUDDIContextService(final JAXRConfiguration config) {
        this.context = new StandardContext();
    }

    @Override
    public synchronized void start(StartContext startContext) throws StartException {
        HttpManagement httpManagement = httpManagementInjector.getOptionalValue();
        try {
            context.setPath("jaxr");
            context.addLifecycleListener(new ContextConfig());
            context.setDocBase(""); // [TODO] Define JAXR doc base

            final Loader loader = new WebCtxLoader(this.getClass().getClassLoader());
            Host host = hostInjector.getValue().getHost();
            loader.setContainer(host);
            context.setLoader(loader);
            context.setInstanceManager(new LocalInstanceManager(httpManagement));

            /*
            context.setReplaceWelcomeFiles(true);
            if (httpManagement != null) {
                context.addWelcomeFile("index.html");
            } else {
                context.addWelcomeFile("index_noconsole.html");
            }
            */

            // Add the JUDDIServlet
            HttpServlet servlet = new JUDDIServlet();
            Wrapper wrapper = context.createWrapper();
            wrapper.setName("JUDDIServlet");
            wrapper.setServlet(servlet);
            wrapper.setServletClass(servlet.getClass().getName());
            context.addChild(wrapper);

            context.addServletMapping("/juddi", "JUDDIServlet");

            host.addChild(context);
            context.create();
        } catch (Exception e) {
            throw new StartException("failed to create context", e);
        }
        try {
            context.start();
        } catch (LifecycleException e) {
            throw new StartException("failed to start context", e);
        }
    }

    @Override
    public synchronized void stop(StopContext stopContext) {
        try {
            hostInjector.getValue().getHost().removeChild(context);
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

    @Override
    public synchronized Context getValue() throws IllegalStateException {
        final Context context = this.context;
        if (context == null) {
            throw new IllegalStateException();
        }
        return context;
    }

    private static class LocalInstanceManager implements InstanceManager {
        private final HttpManagement httpManagement;

        LocalInstanceManager(HttpManagement httpManagement) {
            this.httpManagement = httpManagement;
        }

        @Override
        public Object newInstance(String className) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException, ClassNotFoundException {
            if (className.equals(JUDDIServlet.class.getName()) == false) {
                return Class.forName(className).newInstance();
            }
            JUDDIServlet servlet = new JUDDIServlet();
            return servlet;
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

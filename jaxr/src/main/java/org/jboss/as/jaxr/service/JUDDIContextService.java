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
import org.apache.juddi.registry.RegistryServlet;
import org.apache.naming.resources.ProxyDirContext;
import org.apache.naming.resources.Resource;
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

import javax.naming.Binding;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.servlet.http.HttpServlet;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.util.Properties;

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
        context = new StandardContext() {
            private DirContext resources;

            // [TODO] AS7-2499 Remove DirContext hack to load juddi.properties
            public DirContext getResources() {
                if (resources == null) {
                    DirContext orgdirctx = super.getResources();
                    if (orgdirctx != null) {
                        resources = new JAXRDirContext(new Hashtable<String, Object>(), orgdirctx);
                    }
                }
                return resources;
            }
        };
    }

    @Override
    public synchronized void start(StartContext startContext) throws StartException {
        HttpManagement httpManagement = httpManagementInjector.getOptionalValue();
        try {
            context.setPath("/juddi");
            context.addLifecycleListener(new ContextConfig());
            context.setDocBase(""); // [TODO] Define JAXR doc base

            final Loader loader = new WebCtxLoader(this.getClass().getClassLoader());
            Host host = hostInjector.getValue().getHost();
            loader.setContainer(host);
            context.setLoader(loader);
            context.setInstanceManager(new LocalInstanceManager(httpManagement));

            // [TODO] AS7-2391 Add welcome page to JUDDI context
            //context.setReplaceWelcomeFiles(true);
            //context.addWelcomeFile("index.html");

            // Add the JUDDIServlet
            HttpServlet servlet = new JUDDIServlet();
            Wrapper wrapper = context.createWrapper();
            wrapper.setName("JUDDIServlet");
            wrapper.setServlet(servlet);
            wrapper.setServletClass(servlet.getClass().getName());
            context.addChild(wrapper);

            context.addServletMapping("/publish", "JUDDIServlet");
            context.addServletMapping("/inquiry", "JUDDIServlet");

            // Add the RegistryServlet
            servlet = new RegistryServlet();
            wrapper = context.createWrapper();
            wrapper.setName("JUDDIRegistryServlet");
            wrapper.setServlet(servlet);
            wrapper.setServletClass(servlet.getClass().getName());
            wrapper.setLoadOnStartup(1);
            context.addChild(wrapper);

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

    private static class JAXRDirContext extends ProxyDirContext {
        public JAXRDirContext(Hashtable<String, Object> env, DirContext dircontext) {
            super(env, dircontext);
        }

        @Override
        // [TODO] AS7-2499 Remove DirContext hack to load juddi.properties
        public Object lookup(String name) throws NamingException {
            NamingException namingExeption;
            try {
                return super.lookup(name);
            } catch (NamingException ex) {
                namingExeption = ex;
            }

            InputStream stream = getClass().getClassLoader().getResourceAsStream(name);
            if (stream == null)
                throw namingExeption;

            // Add the juddi.dataSource from the domain model
            if (name.equals("/WEB-INF/juddi.properties")) {
                try {
                    Properties props = new Properties();
                    props.load(stream);
                    JAXRConfiguration config = JAXRConfiguration.INSTANCE;
                    props.setProperty("juddi.dataSource", config.getDataSourceUrl());
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    props.store(out, "jUDDI Registry Properties");
                    stream = new ByteArrayInputStream(out.toByteArray());
                } catch (IOException ex) {
                    NamingException namingException = new NamingException("Cannot append jUDDI datasource");
                    namingException.initCause(ex);
                    throw namingException;
                }
            }
            return new Resource(stream);
        }
    }
}

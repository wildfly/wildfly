/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.osgi.httpservice;

import static org.jboss.as.web.WebLogger.WEB_LOGGER;

import java.util.Dictionary;
import java.util.Enumeration;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.catalina.Host;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.apache.tomcat.util.http.mapper.Mapper;
import org.jboss.as.osgi.OSGiMessages;
import org.jboss.as.osgi.httpservice.HttpServiceFactory.GlobalRegistry;
import org.jboss.as.osgi.httpservice.HttpServiceFactory.Registration;
import org.jboss.as.osgi.httpservice.HttpServiceFactory.Registration.Type;
import org.jboss.as.web.WebServer;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

/**
 * An {@link HttpService} implementation
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Jul-2012
 */
final class HttpServiceImpl implements HttpService {

    private final GlobalRegistry registry;
    private final StandardContext context;
    private final WebServer webServer;
    private final Host virtualHost;
    private final Bundle bundle;

    HttpServiceImpl(StandardContext context, WebServer webServer, Host virtualHost, Bundle bundle) {
        this.registry = GlobalRegistry.INSTANCE;
        this.virtualHost = virtualHost;
        this.webServer = webServer;
        this.context = context;
        this.bundle = bundle;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void registerServlet(String alias, Servlet servlet, Dictionary initparams, HttpContext httpContext) throws ServletException, NamespaceException {

        synchronized (registry) {
            String error = validateAlias(alias, false);
            if (error != null)
                throw new NamespaceException(error);

            if (httpContext == null)
                httpContext = createDefaultHttpContext();

            String wrapperName = alias.substring(1);
            Wrapper wrapper = context.createWrapper();
            wrapper.setName(wrapperName);
            wrapper.setServlet(servlet);
            wrapper.setServletClass(servlet.getClass().getName());

            // Init parameters
            if (initparams != null) {
                Enumeration keys = initparams.keys();
                while(keys.hasMoreElements()) {
                    String key = (String) keys.nextElement();
                    String val = (String) initparams.get(key);
                    wrapper.addInitParameter(key, val);
                }
            }

            registerInternal(registry.register(alias, bundle, wrapper, Type.SERVLET));
            wrapper.allocate(); // Causes servlet.init() to be called, which must be done before we return
        }
    }

    @Override
    public void registerResources(String alias, String name, HttpContext httpContext) throws NamespaceException {

        synchronized (registry) {
            String error = validateAlias(alias, false);
            if (error == null)
                error = validateName(name);
            if (error != null)
                throw new NamespaceException(error);

            if (httpContext == null)
                httpContext = createDefaultHttpContext();

            String wrapperName = alias.substring(1);
            Wrapper wrapper = context.createWrapper();
            wrapper.setName(wrapperName);
            wrapper.setServlet(new ResourceServlet(name, httpContext));
            wrapper.setServletClass(ResourceServlet.class.getName());

            registerInternal(registry.register(alias, bundle, wrapper, Type.RESOURCE));
        }
    }

    @Override
    public void unregister(String alias) {

        synchronized (registry) {

            String error = validateAlias(alias, true);
            if (error != null) {
                WEB_LOGGER.errorf(error);
                return;
            }

            Registration reg = registry.unregister(alias, bundle);
            if (reg != null) {
                unregisterInternal(reg);
            }
        }
    }

    @Override
    public HttpContext createDefaultHttpContext() {
        return new DefaultHttpContext(bundle);
    }

    void registerInternal(Registration reg) {

        Wrapper wrapper = reg.getWrapper();
        String alias = reg.getAlias() + "/*";

        context.addChild(wrapper);
        context.addServletMapping(alias, wrapper.getName());

        // Must be added to the main mapper as no dynamic servlets usually
        Mapper mapper = webServer.getService().getMapper();
        mapper.addWrapper(virtualHost.getName(), context.getPath(), alias, wrapper, false);
    }

    void unregisterInternal(Registration reg) {

        Wrapper wrapper = reg.getWrapper();
        String alias = reg.getAlias() + "/*";

        context.removeChild(wrapper);
        context.removeServletMapping(alias);

        Mapper mapper = webServer.getService().getMapper();
        mapper.removeWrapper(virtualHost.getName(), context.getPath(), alias);
    }

    private String validateAlias(String alias, boolean exists) {

        // An alias must begin with slash ('/') and must not end with slash ('/'), with the exception
        // that an alias of the form "/" is used to denote the root alias
        if (alias == null || !alias.startsWith("/"))
            return OSGiMessages.MESSAGES.invalidServletAlias(alias);
        if (alias.length() > 1 && alias.endsWith("/"))
            return OSGiMessages.MESSAGES.invalidServletAlias(alias);

        if (exists && !registry.exists(alias))
            return OSGiMessages.MESSAGES.aliasMappingDoesNotExist(alias);
        if (!exists && registry.exists(alias))
            return OSGiMessages.MESSAGES.aliasMappingAlreadyExists(alias);

        return null;
    }

    private String validateName(String name) {

        // The name parameter must also not end with slash ('/') with the exception
        // that a name of the form "/" is used to denote the root of the bundle.
        if (name == null || (name.length() > 1 && name.endsWith("/")))
            return OSGiMessages.MESSAGES.invalidResourceName(name);

        // [TODO] remove this restriction
        if (!name.startsWith("/"))
            return OSGiMessages.MESSAGES.invalidResourceName(name);

        return null;
    }
}
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

import java.io.File;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.WeakHashMap;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.ApplicationContext;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.ContextConfig;
import org.apache.tomcat.util.http.mapper.Mapper;
import org.jboss.as.osgi.OSGiMessages;
import org.jboss.as.osgi.httpservice.HttpServiceFactory.GlobalRegistry;
import org.jboss.as.osgi.httpservice.HttpServiceFactory.Registration;
import org.jboss.as.osgi.httpservice.HttpServiceFactory.Registration.Type;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.web.WebMessages;
import org.jboss.as.web.WebServer;
import org.jboss.as.web.deployment.WebCtxLoader;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

/**
 * An {@link HttpService} implementation
 *
 * @author Thomas.Diesler@jboss.com
 * @author David Bosschaert
 * @since 19-Jul-2012
 */
final class HttpServiceImpl implements HttpService {

    private final GlobalRegistry registry;
    private final ServerEnvironment serverEnvironment;
    private final WebServer webServer;
    private final Host virtualHost;
    private final Bundle bundle;

    // This map holds the shared ApplicationContexts to be used with the associated HttpContext.
    // It is a WeakHashMap which means that the ApplicationContexts are remembered for as long
    // as the HttpContext exists.
    private final Map<HttpContext, ApplicationContext> contexts = new WeakHashMap<HttpContext, ApplicationContext>();

    HttpServiceImpl(ServerEnvironment serverEnvironment, WebServer webServer, Host virtualHost, Bundle bundle) {
        this.registry = GlobalRegistry.INSTANCE;
        this.virtualHost = virtualHost;
        this.webServer = webServer;
        this.serverEnvironment = serverEnvironment;
        this.bundle = bundle;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void registerServlet(String alias, Servlet servlet, Dictionary initparams, HttpContext httpContext) throws ServletException, NamespaceException {
        validateAlias(alias, false);
        validateServlet(servlet);

        Wrapper wrapper = registerInternal(alias, servlet, initparams, httpContext, Type.SERVLET);
        wrapper.allocate(); // Causes servlet.init() to be called, which must be done before we return
    }

    @Override
    public void registerResources(String alias, String name, HttpContext httpContext) throws NamespaceException {
        validateAlias(alias, false);
        validateName(name);

        if (httpContext == null)
            httpContext = createDefaultHttpContext();
        ResourceServlet servlet = new ResourceServlet(name, httpContext);

        registerInternal(alias, servlet, null, null, Type.RESOURCE);
    }

    @SuppressWarnings("rawtypes")
    private synchronized Wrapper registerInternal(String alias, Servlet servlet, Dictionary initparams, HttpContext httpContext, Type type) throws NamespaceException {
        File storageDir = new File(serverEnvironment.getServerTempDir() + File.separator + alias + File.separator + "osgiservlet-root");
        storageDir.mkdirs();

        ShareableContext ctx;
        ApplicationContext actx = null;
        if (httpContext != null) {
            actx = contexts.get(httpContext);
            ctx = new ShareableContext(actx);
        } else {
            ctx = new ShareableContext(null);
        }

        ctx.setDocBase(storageDir.getPath());
        ctx.setPath(alias);
        ctx.addLifecycleListener(new ContextConfig());
        WebCtxLoader loader = new WebCtxLoader(servlet.getClass().getClassLoader());
        loader.setContainer(virtualHost);
        ctx.setLoader(loader);

        ctx.addMimeMapping("html", "text/html");
        ctx.addMimeMapping("jpg", "image/jpeg");
        ctx.addMimeMapping("png", "image/png");
        ctx.addMimeMapping("gif", "image/gif");
        ctx.addMimeMapping("css", "text/css");
        ctx.addMimeMapping("js", "text/javascript");

        virtualHost.addChild(ctx);

        WEB_LOGGER.registerWebapp(ctx.getName());
        try {
            ctx.create();
        } catch (Exception ex) {
            throw new NamespaceException(WebMessages.MESSAGES.createContextFailed(), ex);
        }
        try {
            ctx.start();
        } catch (LifecycleException ex) {
            throw new NamespaceException(WebMessages.MESSAGES.startContextFailed(), ex);
        }

        String wrapperName = alias.substring(1);
        Wrapper wrapper = ctx.createWrapper();
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
        registry.register(alias, bundle, ctx, wrapper, type);

        String pattern = "/*";
        ctx.addChild(wrapper);
        ctx.addServletMapping(pattern, wrapper.getName());

        // Must be added to the main mapper as no dynamic servlets usually
        Mapper mapper = webServer.getService().getMapper();
        mapper.addWrapper(virtualHost.getName(), ctx.getPath(), pattern, wrapper, false);

        if (httpContext != null && actx == null) {
            // We have a new shared context, save it for later use

            // The shared Servlet Context is put on a weak has map, which means that
            // as soon as the last instance of a particular httpContext is gone, the
            // shared context is garbage collected too.
            contexts.put(httpContext, ctx.getApplicationContext());
        }

        return wrapper;
    }

    @Override
    public void unregister(String alias) {
        try {
            validateAlias(alias, true);
        } catch (NamespaceException e) {
            WEB_LOGGER.errorf(e, "");
            return;
        }

        Registration reg = registry.unregister(alias, bundle);
        if (reg != null) {
            unregisterInternal(reg);
        }
    }

    @Override
    public HttpContext createDefaultHttpContext() {
        return new DefaultHttpContext(bundle);
    }

    void unregisterInternal(Registration reg) {
        StandardContext context = reg.getContext();
        try {
            context.stop();
        } catch (LifecycleException e) {
            WEB_LOGGER.stopContextFailed(e);
        }
        try {
            context.destroy();
        } catch (Exception e) {
            WEB_LOGGER.destroyContextFailed(e);
        }
    }

    private void validateAlias(String alias, boolean exists) throws NamespaceException {
        // An alias must begin with slash ('/') and must not end with slash ('/'), with the exception
        // that an alias of the form "/" is used to denote the root alias
        if (alias == null || !alias.startsWith("/"))
            throw new IllegalArgumentException(OSGiMessages.MESSAGES.invalidServletAlias(alias));
        if (alias.length() > 1 && alias.endsWith("/"))
            throw new IllegalArgumentException(OSGiMessages.MESSAGES.invalidServletAlias(alias));

        if (exists && !registry.exists(alias))
            throw new IllegalArgumentException(OSGiMessages.MESSAGES.aliasMappingDoesNotExist(alias));
        if (!exists && registry.exists(alias))
            throw new NamespaceException(OSGiMessages.MESSAGES.aliasMappingAlreadyExists(alias));
    }

    private void validateName(String name) throws NamespaceException {
        // The name parameter must also not end with slash ('/') with the exception
        // that a name of the form "/" is used to denote the root of the bundle.
        if (name == null || (name.length() > 1 && name.endsWith("/")))
            throw new NamespaceException(OSGiMessages.MESSAGES.invalidResourceName(name));
    }

    private void validateServlet(Servlet servlet) throws ServletException {
        // A single servlet instance can only be registered once.
        if (registry.contains(servlet))
            throw new ServletException(OSGiMessages.MESSAGES.servletAlreadyRegistered(servlet.getServletInfo()));
    }

    static class ShareableContext extends StandardContext {
        ShareableContext(ApplicationContext existingContext) {
            context = existingContext;
        }

        ApplicationContext getApplicationContext() {
            if (context == null)
                // initialize the servlet context
                getServletContext();

            return context; // will not be null at this point
        }
    }
}
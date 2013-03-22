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

import static org.jboss.as.osgi.httpservice.WebLogger.WEB_LOGGER;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.WeakHashMap;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.osgi.OSGiMessages;
import org.jboss.as.osgi.httpservice.HttpServiceFactory.GlobalRegistry;
import org.jboss.as.osgi.httpservice.HttpServiceFactory.Registration;
import org.jboss.as.osgi.httpservice.HttpServiceFactory.Registration.Type;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.web.host.ApplicationContextWrapper;
import org.jboss.as.web.host.CommonWebServer;
import org.jboss.as.web.host.ServletBuilder;
import org.jboss.as.web.host.WebDeploymentBuilder;
import org.jboss.as.web.host.WebDeploymentController;
import org.jboss.as.web.host.WebHost;
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
    private final CommonWebServer webServer;
    private final WebHost virtualHost;
    private final Bundle bundle;

    // This map holds the shared ApplicationContexts to be used with the associated HttpContext.
    // It is a WeakHashMap which means that the ApplicationContexts are remembered for as long
    // as the HttpContext exists.
    private final Map<HttpContext, ShareableContextWrapper> contexts = new WeakHashMap<HttpContext, ShareableContextWrapper>();

    HttpServiceImpl(ServerEnvironment serverEnvironment, CommonWebServer webServer, WebHost virtualHost, Bundle bundle) {
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

        registerInternal(alias, servlet, initparams, httpContext, Type.SERVLET);

    }

    @Override
    public void registerResources(String alias, String name, HttpContext httpContext) throws NamespaceException {
        validateAlias(alias, false);
        validateName(name);

        if (httpContext == null) { httpContext = createDefaultHttpContext(); }
        ResourceServlet servlet = new ResourceServlet(name, httpContext);

        registerInternal(alias, servlet, null, null, Type.RESOURCE);
    }

    @SuppressWarnings("rawtypes")
    private synchronized ServletBuilder registerInternal(String alias, Servlet servlet, Dictionary initparams, HttpContext httpContext, Type type) throws NamespaceException {
        File storageDir = new File(serverEnvironment.getServerTempDir() + File.separator + alias + File.separator + "osgiservlet-root");
        storageDir.mkdirs();

        //ShareableContext ctx;
        WebDeploymentBuilder deploymentBuilder = new WebDeploymentBuilder();
        ShareableContextWrapper scWrapper = null;
        if (httpContext != null) {
            scWrapper = contexts.get(httpContext);
        } else {
            scWrapper = new ShareableContextWrapper();
            httpContext = new DefaultHttpContext(bundle);
        }
        if(scWrapper == null) {
            contexts.put(httpContext, scWrapper);
        }


        deploymentBuilder.setDocumentRoot(storageDir);
        deploymentBuilder.setContextRoot(alias);
        deploymentBuilder.setApplicationContextWrapper(scWrapper);
        //ctx.addLifecycleListener(new ContextConfig());

        deploymentBuilder.setClassLoader(servlet.getClass().getClassLoader());

        deploymentBuilder.addMimeMapping("html", "text/html");
        deploymentBuilder.addMimeMapping("jpg", "image/jpeg");
        deploymentBuilder.addMimeMapping("png", "image/png");
        deploymentBuilder.addMimeMapping("gif", "image/gif");
        deploymentBuilder.addMimeMapping("css", "text/css");
        deploymentBuilder.addMimeMapping("js", "text/javascript");

        String wrapperName = alias.substring(1);
        ServletBuilder wrapper = new ServletBuilder();

        wrapper.setServletName(wrapperName);
        wrapper.setServlet(new SecurityServletWrapper(servlet, httpContext));
        wrapper.setServletClass(servlet.getClass());

        // Init parameters
        if (initparams != null) {
            Enumeration keys = initparams.keys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                String val = (String) initparams.get(key);
                wrapper.addInitParam(key, val);
            }
        }

        wrapper.setForceInit(true);
        wrapper.addUrlMapping("/*");
        deploymentBuilder.addServlet(wrapper);

        WebDeploymentController deploymentController;
        try {
            deploymentController = virtualHost.addWebDeployment(deploymentBuilder);
            WEB_LOGGER.registerWebapp(deploymentBuilder.getContextRoot());
            deploymentController.create();
        } catch (Exception ex) {
            throw new NamespaceException(WEB_LOGGER.createContextFailed(), ex);
        }
        try {
            deploymentController.start();
        } catch (Exception ex) {
            throw new NamespaceException(WEB_LOGGER.startContextFailed(), ex);
        }

        registry.register(alias, bundle, deploymentController, servlet, type);

        // Must be added to the main mapper as no dynamic servlets usually
        /*Mapper mapper = webServer.getService().getMapper();
        mapper.addWrapper(virtualHost.getName(), ctx.getPath(), pattern, wrapper, false);*/

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
        WebDeploymentController context = reg.getContext();
        try {
            context.stop();
        } catch (Exception e) {
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
        if (alias == null || !alias.startsWith("/")) { throw new IllegalArgumentException(OSGiMessages.MESSAGES.invalidServletAlias(alias)); }
        if (alias.length() > 1 && alias.endsWith("/")) { throw new IllegalArgumentException(OSGiMessages.MESSAGES.invalidServletAlias(alias)); }

        if (exists && !registry.exists(alias)) { throw new IllegalArgumentException(OSGiMessages.MESSAGES.aliasMappingDoesNotExist(alias)); }
        if (!exists && registry.exists(alias)) { throw new NamespaceException(OSGiMessages.MESSAGES.aliasMappingAlreadyExists(alias)); }
    }

    private void validateName(String name) throws NamespaceException {
        // The name parameter must also not end with slash ('/') with the exception
        // that a name of the form "/" is used to denote the root of the bundle.
        if (name == null || (name.length() > 1 && name.endsWith("/"))) { throw new NamespaceException(OSGiMessages.MESSAGES.invalidResourceName(name)); }
    }

    private void validateServlet(Servlet servlet) throws ServletException {
        // A single servlet instance can only be registered once.
        if (registry.contains(servlet)) { throw new ServletException(OSGiMessages.MESSAGES.servletAlreadyRegistered(servlet.getServletInfo())); }
    }

    static class ShareableContextWrapper implements ApplicationContextWrapper {

        private Object sharedContext;

        @Override
        public synchronized Object wrap(final Object context) {
            if(sharedContext == null) {
                this.sharedContext = context;
            }
            return sharedContext;
        }
    }

    /* This wrapper class takes care of handling the security through the HttpContext.
     */
    static class SecurityServletWrapper implements Servlet {
        private final HttpContext httpContext;
        private final Servlet delegate;

        SecurityServletWrapper(Servlet servlet, HttpContext ctx) {
            if (servlet == null) { throw new NullPointerException(); }
            delegate = servlet;

            if (ctx == null) { throw new NullPointerException(); }
            httpContext = ctx;
        }

        @Override
        public void destroy() {
            delegate.destroy();
        }

        @Override
        public ServletConfig getServletConfig() {
            return delegate.getServletConfig();
        }

        @Override
        public String getServletInfo() {
            return delegate.getServletInfo();
        }

        @Override
        public void init(ServletConfig sc) throws ServletException {
            delegate.init(sc);
        }

        @Override
        public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
            if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                if (!httpContext.handleSecurity(httpRequest, httpResponse)) { return; }

                Object u = httpRequest.getAttribute(HttpContext.REMOTE_USER);
                String remoteUser = u instanceof String ? (String) u : null;
                Object a = httpRequest.getAttribute(HttpContext.AUTHENTICATION_TYPE);
                String authType = a instanceof String ? (String) a : null;

                if (remoteUser != null || authType != null) {
                    request = new SecurityRequestWrapper(remoteUser, authType, httpRequest);
                }
            }
            delegate.service(request, response);
        }
    }

    /* This wrapper class can provide the remote user and auth type information if provided through the HttpContext.
     */
    static class SecurityRequestWrapper extends HttpServletRequestWrapper implements HttpServletRequest {
        private final String remoteUser;
        private final String authType;

        SecurityRequestWrapper(String remoteUser, String authType, HttpServletRequest request) {
            super(request);
            this.remoteUser = remoteUser;
            this.authType = authType;
        }

        @Override
        public String getAuthType() {
            return authType;
        }

        @Override
        public String getRemoteUser() {
            return remoteUser;
        }
    }
}
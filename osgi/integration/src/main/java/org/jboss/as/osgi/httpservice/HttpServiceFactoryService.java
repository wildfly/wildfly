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
import static org.jboss.as.web.WebSubsystemServices.JBOSS_WEB;

import java.io.File;
import java.util.Hashtable;

import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Loader;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.ContextConfig;
import org.apache.tomcat.InstanceManager;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.as.server.mgmt.HttpManagementService;
import org.jboss.as.server.mgmt.domain.HttpManagement;
import org.jboss.as.web.VirtualHost;
import org.jboss.as.web.WebMessages;
import org.jboss.as.web.WebServer;
import org.jboss.as.web.WebSubsystemServices;
import org.jboss.as.web.deployment.WebCtxLoader;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.framework.Services;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;

/**
 * A service that installs a {@link ServiceFactory} for {@link HttpService}
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Jul-2012
 */
public final class HttpServiceFactoryService extends AbstractService<StandardContext> {

    static final ServiceName JBOSS_WEB_HTTPSERVICE_FACTORY = JBOSS_WEB.append("httpservice", "factory");

    // [TODO] Make these configurable
    static final String VIRTUAL_HOST = "default-host";
    static final String CONTEXT_NAME = "/httpservice";

    private final InjectedValue<ServerEnvironment> injectedServerEnvironment = new InjectedValue<ServerEnvironment>();
    private final InjectedValue<BundleContext> injectedSystemContext = new InjectedValue<BundleContext>();
    private final InjectedValue<PathManager> injectedPathManager = new InjectedValue<PathManager>();
    private final InjectedValue<VirtualHost> injectedVirtualHost = new InjectedValue<VirtualHost>();
    private final InjectedValue<HttpManagement> injectedHttpManagement = new InjectedValue<HttpManagement>();
    private final InjectedValue<WebServer> injectedWebServer = new InjectedValue<WebServer>();
    private final StandardContext context;
    private ServiceRegistration registration;

    public static ServiceController<StandardContext> addService(ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
        HttpServiceFactoryService service = new HttpServiceFactoryService();
        ServiceBuilder<StandardContext> builder = serviceTarget.addService(JBOSS_WEB_HTTPSERVICE_FACTORY, service);
        builder.addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, service.injectedServerEnvironment);
        builder.addDependency(PathManagerService.SERVICE_NAME, PathManager.class, service.injectedPathManager);
        builder.addDependency(WebSubsystemServices.JBOSS_WEB_HOST.append(VIRTUAL_HOST), VirtualHost.class, service.injectedVirtualHost);
        builder.addDependency(WebSubsystemServices.JBOSS_WEB, WebServer.class, service.injectedWebServer);
        builder.addDependency(DependencyType.OPTIONAL, HttpManagementService.SERVICE_NAME, HttpManagement.class, service.injectedHttpManagement);
        builder.addDependency(Services.FRAMEWORK_ACTIVE, BundleContext.class, service.injectedSystemContext);
        builder.addListener(verificationHandler);
        builder.setInitialMode(Mode.PASSIVE);
        return builder.install();
    }

    private HttpServiceFactoryService() {
        this.context = new StandardContext();
    }

    @Override
    public void start(StartContext startContext) throws StartException {

        ServerEnvironment env = injectedServerEnvironment.getValue();
        Host virtualHost = injectedVirtualHost.getValue().getHost();
        BundleContext syscontext = injectedSystemContext.getValue();
        WebServer webServer = injectedWebServer.getValue();

        File storageDir = new File(env.getServerTempDir() + File.separator + CONTEXT_NAME + File.separator + "httpservice-root");
        context.setDocBase(storageDir.getPath());
        storageDir.mkdirs();

        context.setPath(CONTEXT_NAME);
        context.addLifecycleListener(new ContextConfig());
        Loader loader = new WebCtxLoader(getClass().getClassLoader());
        loader.setContainer(virtualHost);
        context.setLoader(loader);
        context.setInstanceManager(new LocalInstanceManager());

        context.addMimeMapping("html", "text/html");
        context.addMimeMapping("jpg", "image/jpeg");
        context.addMimeMapping("png", "image/png");
        context.addMimeMapping("gif", "image/gif");
        context.addMimeMapping("css", "text/css");
        context.addMimeMapping("js", "text/javascript");

        virtualHost.addChild(context);

        WEB_LOGGER.registerWebapp(context.getName());
        try {
            context.create();
        } catch (Exception ex) {
            throw new StartException(WebMessages.MESSAGES.createContextFailed(), ex);
        }
        try {
            context.start();
        } catch (LifecycleException ex) {
            throw new StartException(WebMessages.MESSAGES.startContextFailed(), ex);
        }

        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_RANKING, Integer.MIN_VALUE);
        props.put("provider", getClass().getPackage().getName());

        ServiceFactory serviceFactory = new HttpServiceFactory(webServer, virtualHost, context);
        registration = syscontext.registerService(HttpService.class.getName(), serviceFactory, props);
    }

    @Override
    public void stop(StopContext stopContext) {
        registration.unregister();
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

    @Override
    public StandardContext getValue() throws IllegalStateException {
        return context;
    }

    private static class LocalInstanceManager implements InstanceManager {

        LocalInstanceManager() {
        }

        @Override
        public Object newInstance(String className) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object newInstance(String className, ClassLoader classLoader) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object newInstance(Class<?> clazz) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void newInstance(Object instance) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void destroyInstance(Object instance) {
        }
    }
}

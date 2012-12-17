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
package org.jboss.as.osgi.web;

import static org.jboss.as.web.WebLogger.WEB_LOGGER;
import static org.jboss.as.web.WebMessages.MESSAGES;
import static org.jboss.as.web.WebSubsystemServices.JBOSS_WEB;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletContext;

import org.apache.catalina.core.StandardContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.web.deployment.WebDeploymentService.ContextActivator;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.deployment.interceptor.AbstractLifecycleInterceptor;
import org.jboss.osgi.deployment.interceptor.InvocationContext;
import org.jboss.osgi.deployment.interceptor.LifecycleInterceptor;
import org.jboss.osgi.deployment.interceptor.LifecycleInterceptorException;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.resolver.XBundle;
import org.jboss.osgi.resolver.XBundleRevision;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * {@link LifecycleInterceptor} for webapp bundles.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 26-Jun-2012
 */
class WebContextLifecycleInterceptor extends AbstractLifecycleInterceptor implements Service<LifecycleInterceptor> {

    static final ServiceName SERVICE_NAME = JBOSS_WEB.append(WebContextLifecycleInterceptor.class.getSimpleName());

    private final InjectedValue<BundleContext> injectedSystemContext = new InjectedValue<BundleContext>();
    private ServiceRegistration registration;

    static ServiceController<LifecycleInterceptor> addService(ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
        WebContextLifecycleInterceptor service = new WebContextLifecycleInterceptor();
        ServiceBuilder<LifecycleInterceptor> builder = serviceTarget.addService(SERVICE_NAME, service);
        builder.addDependency(Services.FRAMEWORK_CREATE, BundleContext.class, service.injectedSystemContext);
        builder.addListener(verificationHandler);
        builder.setInitialMode(Mode.ON_DEMAND);
        return builder.install();
    }

    @Override
    public void start(StartContext context) throws StartException {
        BundleContext syscontext = injectedSystemContext.getValue();
        registration = syscontext.registerService(LifecycleInterceptor.class.getName(), this, null);
    }

    @Override
    public void stop(StopContext context) {
        if (registration != null)
            registration.unregister();
    }

    @Override
    public void invoke(int state, InvocationContext context) {
        XBundle bundle = (XBundle) context.getBundle();
        XBundleRevision brev = bundle.getBundleRevision();
        ContextActivator activator = brev.getAttachment(ContextActivator.class);
        if (activator != null) {
            switch (state) {
                case Bundle.ACTIVE:
                    try {
                        injectBundleContext(activator.getContext(), bundle.getBundleContext());
                        if (!activator.start(4, TimeUnit.SECONDS)) {
                            throw new LifecycleInterceptorException(MESSAGES.startContextFailed());
                        }
                    } catch (TimeoutException ex) {
                        throw new LifecycleInterceptorException(ex.getMessage(), ex);
                    }
                    break;
                case Bundle.RESOLVED:
                    activator.stop(4, TimeUnit.SECONDS);
                    uninjectBundleContext(activator.getContext());
                    break;
            }
        }
    }

    private void injectBundleContext(StandardContext webContext, BundleContext bundleContext) {
        WEB_LOGGER.debugf("Injecting bundle context %s into %s", bundleContext, webContext);
        ServletContext servletContext = webContext.getServletContext();
        servletContext.setAttribute(WebExtension.OSGI_BUNDLECONTEXT, bundleContext);
        registerServletContextService(servletContext, bundleContext);
    }

    private void uninjectBundleContext(StandardContext webContext) {
        WEB_LOGGER.debugf("Uninjecting bundle context from %s", webContext);
        ServletContext servletContext = webContext.getServletContext();
        servletContext.removeAttribute(WebExtension.OSGI_BUNDLECONTEXT);
    }

    private void registerServletContextService(ServletContext servletContext, BundleContext bundleContext) {
        Bundle bundle = bundleContext.getBundle();
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("osgi.web.symbolicname", bundle.getSymbolicName());
        Dictionary<?, ?> headers = bundle.getHeaders();
        // The version of the Web Application Bundle. If no Bundle-Version is specified in the manifest then
        // this property must not be set.
        Object version = headers.get(Constants.BUNDLE_VERSION);
        if (version instanceof String)
            props.put("osgi.web.version", version);

        Object contextPath = headers.get(WebExtension.WEB_CONTEXTPATH);
        if (contextPath instanceof String)
            props.put("osgi.web.contextpath", contextPath);

        bundleContext.registerService(ServletContext.class.getName(), servletContext, props);
    }

    @Override
    public LifecycleInterceptor getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}
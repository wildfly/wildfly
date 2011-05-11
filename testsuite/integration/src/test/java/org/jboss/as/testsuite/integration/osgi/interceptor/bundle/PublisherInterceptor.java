/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
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
package org.jboss.as.testsuite.integration.osgi.interceptor.bundle;

import javax.servlet.http.HttpServlet;

import org.jboss.logging.Logger;
import org.jboss.osgi.deployment.interceptor.AbstractLifecycleInterceptor;
import org.jboss.osgi.deployment.interceptor.InvocationContext;
import org.jboss.osgi.deployment.interceptor.LifecycleInterceptorException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;

/**
 * An interceptor that publishes HttpMetadata.
 * 
 * @author thomas.diesler@jboss.com
 * @since 23-Oct-2009
 */
public class PublisherInterceptor extends AbstractLifecycleInterceptor {
    // Provide logging
    private static final Logger log = Logger.getLogger(PublisherInterceptor.class);

    PublisherInterceptor() {
        // Add the required input
        addInput(HttpMetadata.class);
    }

    public void invoke(int state, InvocationContext context) {
        // HttpMetadata is guaratied to be available because we registered
        // this type as required input
        HttpMetadata metadata = context.getAttachment(HttpMetadata.class);

        // Register HttpMetadata on STARTING
        if (state == Bundle.STARTING) {
            String servletName = metadata.getServletName();
            try {
                log.info("Publish HttpMetadata: " + metadata);

                // Load the endpoint servlet from the bundle
                Bundle bundle = context.getBundle();
                Class<?> servletClass = bundle.loadClass(servletName);
                HttpServlet servlet = (HttpServlet) servletClass.newInstance();

                // Register the servlet with the HttpService
                HttpService httpService = getHttpService(context, true);
                httpService.registerServlet("/servlet", servlet, null, null);
            } catch (RuntimeException rte) {
                throw rte;
            } catch (Exception ex) {
                throw new LifecycleInterceptorException("Cannot publish: " + servletName, ex);
            }
        }

        // Unregister the endpoint on STOPPING
        else if (state == Bundle.STOPPING) {
            log.info("Unpublish HttpMetadata: " + metadata);
            HttpService httpService = getHttpService(context, false);
            if (httpService != null)
                httpService.unregister("/servlet");
        }
    }

    private HttpService getHttpService(InvocationContext context, boolean required) {
        BundleContext bndContext = context.getBundle().getBundleContext();
        ServiceReference sref = bndContext.getServiceReference(HttpService.class.getName());
        if (sref == null && required == true)
            throw new IllegalStateException("Required HttpService not available");

        HttpService httpService = (HttpService) bndContext.getService(sref);
        return httpService;
    }
}

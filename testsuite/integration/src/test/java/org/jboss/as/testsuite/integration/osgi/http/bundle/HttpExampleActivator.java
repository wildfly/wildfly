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
package org.jboss.as.testsuite.integration.osgi.http.bundle;

import java.util.Properties;

import org.jboss.osgi.logging.LogServiceTracker;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A Service Activator
 * 
 * @author thomas.diesler@jboss.com
 * @since 04-Feb-2009
 */
public class HttpExampleActivator implements BundleActivator {
    private ServiceTracker tracker;
    private LogService log;

    public void start(BundleContext context) {
        log = new LogServiceTracker(context);
        log.log(LogService.LOG_INFO, "Start: " + context.getBundle());

        tracker = new ServiceTracker(context, HttpService.class.getName(), null) {
            @Override
            public Object addingService(ServiceReference reference) {
                HttpService httpService = (HttpService) super.addingService(reference);
                registerService(context, httpService);
                return httpService;
            }

            @Override
            public void removedService(ServiceReference reference, Object service) {
                HttpService httpService = (HttpService) service;
                unregisterService(context, httpService);
                super.removedService(reference, service);
            }
        };
        tracker.open();
    }

    public void stop(BundleContext context) {
        HttpService httpService = (HttpService) tracker.getService();
        if (httpService != null)
            unregisterService(context, httpService);
    }

    private void registerService(BundleContext context, HttpService httpService) {
        log.log(LogService.LOG_INFO, "registerService: " + context.getBundle());
        try {
            Properties initParams = new Properties();
            initParams.setProperty("initProp", "SomeValue");
            httpService.registerServlet("/servlet", new EndpointServlet(context), initParams, null);
            httpService.registerResources("/file", "/res", null);
        } catch (Exception ex) {
            throw new RuntimeException("Cannot register context", ex);
        }
    }

    private void unregisterService(BundleContext context, HttpService httpService) {
        log.log(LogService.LOG_INFO, "unregisterService: " + context.getBundle());
        httpService.unregister("/servlet");
        httpService.unregister("/file");
    }

}
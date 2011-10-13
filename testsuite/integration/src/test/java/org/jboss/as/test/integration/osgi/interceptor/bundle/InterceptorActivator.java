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
package org.jboss.as.test.integration.osgi.interceptor.bundle;

import org.jboss.osgi.deployment.interceptor.LifecycleInterceptor;
import org.jboss.osgi.deployment.interceptor.LifecycleInterceptorService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Registers the interceptors with the {@link LifecycleInterceptorService}
 *
 * @author thomas.diesler@jboss.com
 * @since 20-Oct-2009
 */
public class InterceptorActivator implements BundleActivator {
    public void start(BundleContext context) {
        LifecycleInterceptor publisher = new PublisherInterceptor();
        LifecycleInterceptor parser = new ParserInterceptor();

        // Add the interceptors, the order of which is handles by the service
        context.registerService(LifecycleInterceptor.class.getName(), publisher, null);
        context.registerService(LifecycleInterceptor.class.getName(), parser, null);
    }

    public void stop(BundleContext context) {
    }
}
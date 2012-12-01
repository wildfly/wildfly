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

import java.util.concurrent.TimeUnit;
import org.apache.catalina.core.StandardContext;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.osgi.framework.spi.BundleManager;
import org.jboss.osgi.framework.spi.FutureServiceValue;
import org.jboss.osgi.resolver.XBundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;

/**
 * The {@link HttpService} activator
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Oct-2012
 */
public final class HttpBundleActivator implements BundleActivator {

    private ServiceController<StandardContext> controller;

    @Override
    public void start(BundleContext context) throws Exception {
        XBundle sysbundle = (XBundle) context.getBundle();
        BundleManager bundleManager = sysbundle.adapt(BundleManager.class);
        ServiceTarget serviceTarget = bundleManager.getServiceTarget();
        controller = HttpServiceFactoryService.addService(serviceTarget);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (controller != null) {
            // Wait for the {@link HttpServiceFactoryService} to get removed
            // [AS7-5828] HttpService may fail to start due to already existing service
            FutureServiceValue<StandardContext> future = new FutureServiceValue<StandardContext>(controller, State.REMOVED);
            controller.setMode(Mode.REMOVE);
            future.get(10, TimeUnit.SECONDS);
        }
    }
}

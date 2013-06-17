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

package org.jboss.as.osgi.service;

import org.jboss.as.osgi.OSGiConstants;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.osgi.resolver.XEnvironment;
import org.jboss.osgi.resolver.spi.AbstractEnvironment;

/**
 * The standalone {@link XEnvironment} service.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-May-2012
 */
public final class EnvironmentService extends AbstractService<XEnvironment> {

    private XEnvironment environment;

    public static ServiceController<?> addService(final ServiceTarget target) {
        EnvironmentService service = new EnvironmentService();
        ServiceBuilder<?> builder = target.addService(OSGiConstants.ENVIRONMENT_SERVICE_NAME, service);
        return builder.install();
    }

    private EnvironmentService() {
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        environment = new AbstractEnvironment();
    }

    @Override
    public synchronized void stop(StopContext context) {
        environment = null;
    }

    @Override
    public synchronized XEnvironment getValue() throws IllegalStateException {
        return environment;
    }
}

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

import static org.jboss.as.osgi.OSGiLogger.LOGGER;
import static org.jboss.as.osgi.OSGiConstants.SERVICE_BASE_NAME;

import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.resolver.XEnvironment;
import org.jboss.osgi.resolver.XResolver;
import org.jboss.osgi.resolver.felix.StatelessResolver;
import org.osgi.service.resolver.Resolver;

/**
 * The standalone {@link Resolver} service.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 20-Mar-2012
 */
class ResolverService extends AbstractService<Resolver> {

    static final ServiceName RESOLVER_NAME = SERVICE_BASE_NAME.append("resolver");

    private final InjectedValue<XEnvironment> injectedEnvironment = new InjectedValue<XEnvironment>();
    private XResolver resolver;

    static ServiceController<?> addService(final ServiceTarget target) {
        ResolverService service = new ResolverService();
        ServiceBuilder<?> builder = target.addService(RESOLVER_NAME, service);
        builder.addDependency(Services.ENVIRONMENT, XEnvironment.class, service.injectedEnvironment);
        return builder.install();
    }

    private ResolverService() {
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        ServiceController<?> serviceController = context.getController();
        LOGGER.tracef("Starting: %s in mode %s", serviceController.getName(), serviceController.getMode());
        resolver = new StatelessResolver();
    }

    @Override
    public synchronized void stop(StopContext context) {
        ServiceController<?> serviceController = context.getController();
        LOGGER.tracef("Stopping: %s in mode %s", serviceController.getName(), serviceController.getMode());
        resolver = null;
    }

    @Override
    public synchronized Resolver getValue() throws IllegalStateException {
        return resolver;
    }
}

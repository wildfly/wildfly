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
import org.jboss.osgi.resolver.XResolver;
import org.jboss.osgi.resolver.spi.AbstractResolver;

/**
 * The standalone {@link XResolver} service.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-May-2012
 */
public final class AbstractResolverService extends AbstractService<XResolver> {

    private XResolver resolver;

    public static ServiceController<?> addService(final ServiceTarget target) {
        AbstractResolverService service = new AbstractResolverService();
        ServiceBuilder<?> builder = target.addService(OSGiConstants.ABSTRACT_RESOLVER_SERVICE_NAME, service);
        return builder.install();
    }

    private AbstractResolverService() {
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        resolver = new AbstractResolver();
    }

    @Override
    public synchronized void stop(StopContext context) {
        resolver = null;
    }

    @Override
    public synchronized XResolver getValue() throws IllegalStateException {
        return resolver;
    }
}

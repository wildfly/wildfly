/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.osgi.jpa.bundle;

import java.util.concurrent.Callable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author thomas.diesler@jboss.com
 * @since 28-Sep-2012
 */
public class PersistenceActivatorA implements BundleActivator {

    @Override
    public void start(final BundleContext context) throws Exception {
        context.registerService(Callable.class.getName(), new PersistenceService(context), null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }
}

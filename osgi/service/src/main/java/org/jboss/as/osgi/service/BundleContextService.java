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

import org.jboss.as.osgi.parser.SubsystemState.Activation;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.osgi.framework.BundleContext;
import org.osgi.framework.launch.Framework;

/**
 * Service responsible for creating and managing the life-cycle of the OSGi system {@link BundleContext}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 29-Oct-2010
 */
public class BundleContextService implements Service<BundleContext> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("osgi", "context");

    private final InjectedValue<Framework> injectedFramework = new InjectedValue<Framework>();
    private BundleContext systemContext;

    public static void addService(final ServiceTarget target, Activation policy) {
        BundleContextService service = new BundleContextService();
        ServiceBuilder<?> serviceBuilder = target.addService(BundleContextService.SERVICE_NAME, service);
        serviceBuilder.addDependency(FrameworkService.SERVICE_NAME, Framework.class, service.injectedFramework);
        serviceBuilder.setInitialMode(policy == Activation.LAZY ? Mode.ON_DEMAND : Mode.ACTIVE);
        serviceBuilder.install();
    }

    public synchronized void start(final StartContext context) throws StartException {
        Framework framework = injectedFramework.getValue();
        systemContext = framework.getBundleContext();
    }

    public synchronized void stop(StopContext context) {
        systemContext = null;
    }

    @Override
    public BundleContext getValue() throws IllegalStateException {
        return systemContext;
    }
}

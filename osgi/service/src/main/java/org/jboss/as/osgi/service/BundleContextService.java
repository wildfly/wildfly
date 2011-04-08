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

import static org.jboss.as.osgi.service.FrameworkBootstrapService.FRAMEWORK_BASE_NAME;

import org.jboss.as.osgi.parser.SubsystemState.Activation;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.framework.ServiceNames;
import org.osgi.framework.BundleContext;

/**
 * Service responsible for creating and managing the life-cycle of the OSGi system {@link BundleContext}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 29-Oct-2010
 *
 * @deprecated Use {@link ServiceNames#SYSTEM_CONTEXT}
 */
@Deprecated
public class BundleContextService extends AbstractService<BundleContext> {

    @Deprecated
    public static final ServiceName SERVICE_NAME = FRAMEWORK_BASE_NAME.append("systemcontext");

    private final InjectedValue<BundleContext> injectedBundleContext = new InjectedValue<BundleContext>();

    public static void addService(final ServiceTarget target, Activation policy) {
        BundleContextService service = new BundleContextService();
        ServiceBuilder<?> builder = target.addService(BundleContextService.SERVICE_NAME, service);
        builder.addDependency(ServiceNames.SYSTEM_CONTEXT, BundleContext.class, service.injectedBundleContext);
        builder.addDependency(ServiceNames.FRAMEWORK_ACTIVE);
        builder.setInitialMode(policy == Activation.LAZY ? Mode.ON_DEMAND : Mode.ACTIVE);
        builder.install();
    }

    @Override
    public BundleContext getValue() throws IllegalStateException {
        return injectedBundleContext.getValue();
    }
}

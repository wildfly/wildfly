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
package org.jboss.as.osgi.service;

import org.jboss.as.osgi.OSGiConstants;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.framework.spi.EnvironmentPlugin;
import org.jboss.osgi.resolver.XEnvironment;

/**
 * An integation plugin for the {@link XEnvironment}.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-May-2013
 */
public final class EnvironmentIntegration extends EnvironmentPlugin {

    private final InjectedValue<XEnvironment> injectedEnvironment = new InjectedValue<XEnvironment>();

    @Override
    protected void addServiceDependencies(ServiceBuilder<XEnvironment> builder) {
        builder.addDependency(OSGiConstants.ENVIRONMENT_SERVICE_NAME, XEnvironment.class, injectedEnvironment);
        builder.setInitialMode(Mode.ON_DEMAND);
    }

    @Override
    protected XEnvironment createServiceValue(StartContext startContext) {
        return injectedEnvironment.getValue();
    }

}

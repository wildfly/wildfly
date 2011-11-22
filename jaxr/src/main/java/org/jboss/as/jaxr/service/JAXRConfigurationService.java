/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jaxr.service;

import org.jboss.as.jaxr.extension.JAXRConstants;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * The configuration service of the JAXR subsystem.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 22-Nov-2011
 */
public class JAXRConfigurationService extends AbstractService<JAXRConfiguration> {

    public static final ServiceName SERVICE_NAME = JAXRConstants.SERVICE_BASE_NAME.append("configuration");

    private final JAXRConfiguration config;

    public static ServiceController<?> addService(final ServiceTarget target, final JAXRConfiguration config, final ServiceListener<Object>... listeners) {
        JAXRConfigurationService service = new JAXRConfigurationService(config);
        ServiceBuilder<?> builder = target.addService(SERVICE_NAME, service);
        builder.addListener(listeners);
        return builder.install();
    }

    private JAXRConfigurationService(JAXRConfiguration config) {
        this.config = config;
    }

    @Override
    public JAXRConfiguration getValue() throws IllegalStateException {
        return config;
    }
}

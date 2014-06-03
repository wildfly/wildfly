/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.deployment.trivial;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * ServiceActivator that installs itself as a service and sets a set of system
 * properties read from a "service-activator-deployment.properties" resource in the deployment.
 * If no resource is available, sets a default property.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
public class ServiceActivatorDeployment implements ServiceActivator, Service<Void> {

    public static final ServiceName SERVICE_NAME = ServiceName.of("test", "deployment", "trivial");
    public static final String PROPERTIES_RESOURCE = "service-activator-deployment.properties";
    public static final String DEFAULT_SYS_PROP_NAME = "test.deployment.trivial.prop";
    public static final String DEFAULT_SYS_PROP_VALUE = "default-value";

    @Override
    public void activate(ServiceActivatorContext serviceActivatorContext) throws ServiceRegistryException {
        serviceActivatorContext.getServiceTarget().addService(SERVICE_NAME, this).install();
    }

    private final Properties properties = new Properties();

    @Override
    public synchronized void start(StartContext context) throws StartException {
        InputStream is = getClass().getResourceAsStream(PROPERTIES_RESOURCE);
        if (is != null) {
            try {
                properties.load(is);
            } catch (IOException e) {
                throw new StartException(e);
            } finally {
                try {
                    is.close();
                } catch (IOException ignored) {
                    //
                }

            }
        } else {
            properties.setProperty(DEFAULT_SYS_PROP_NAME, DEFAULT_SYS_PROP_VALUE);
        }


        for (String name : properties.stringPropertyNames()) {
            System.setProperty(name, properties.getProperty(name));
        }
    }

    @Override
    public void stop(StopContext context) {
        for (String name : properties.stringPropertyNames()) {
            System.clearProperty(name);
        }
        properties.clear();
    }

    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }
}

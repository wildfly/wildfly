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

import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.framework.Services;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.util.xml.XMLParserActivator;

import java.net.URL;

import static org.jboss.as.osgi.OSGiLogger.ROOT_LOGGER;
import static org.jboss.as.osgi.service.FrameworkBootstrapService.SERVICE_BASE_NAME;

/**
 * An service that provides {@link javax.xml.parsers.SAXParserFactory} and {@link javax.xml.parsers.DocumentBuilderFactory}
 * service to the OSGi system context
 *
 * @author thomas.diesler@jboss.com
 * @since 31-Jan-2012
 */
final class JAXPServiceProvider extends AbstractService<Void> {

    public static final ServiceName SERVICE_NAME = SERVICE_BASE_NAME.append("jaxp.provider");

    private final InjectedValue<BundleContext> injectedSystemContext = new InjectedValue<BundleContext>();
    private XMLParserActivator activator;

    static ServiceController<?> addService(final ServiceTarget target) {
        JAXPServiceProvider service = new JAXPServiceProvider();
        ServiceBuilder<?> builder = target.addService(SERVICE_NAME, service);
        builder.addDependency(Services.SYSTEM_CONTEXT, BundleContext.class, service.injectedSystemContext);
        builder.addDependency(Services.FRAMEWORK_CREATE);
        builder.setInitialMode(Mode.PASSIVE);
        return builder.install();
    }

    private JAXPServiceProvider() {
    }

    @Override
    public void start(StartContext context) throws StartException {
        BundleContext syscontext = injectedSystemContext.getValue();
        try {
            final ClassLoader resloader = getClass().getClassLoader();
            activator = new XMLParserActivator() {
                @Override
                protected URL getResourceURL(Bundle parserBundle, String resname) {
                    return resloader.getResource(resname);
                }
            };
            activator.start(syscontext);
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext context) {
        BundleContext syscontext = injectedSystemContext.getValue();
        if (activator != null) {
            try {
                activator.stop(syscontext);
                activator = null;
            } catch (Exception e) {
                ROOT_LOGGER.warn(e);
            }
        }
    }

    @Override
    public String toString() {
        return JAXPServiceProvider.class.getSimpleName();
    }
}

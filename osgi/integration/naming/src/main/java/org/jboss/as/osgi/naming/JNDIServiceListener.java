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
package org.jboss.as.osgi.naming;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.naming.spi.ObjectFactory;

import org.jboss.as.naming.InitialContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * Registers OSGi Services under the osgi.jndi.url.scheme
 *
 * @author David Bosschaert
 * @author Thomas.Diesler@jboss.com
 * @since 31-Jul-2012
 */
final class JNDIServiceListener implements ServiceListener {

    private static final String OSGI_JNDI_URL_SCHEME = "osgi.jndi.url.scheme";

    private final BundleContext bundleContext;

    public JNDIServiceListener(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        try {
            // Register the pre-existing services
            ServiceReference[] refs = bundleContext.getServiceReferences(ObjectFactory.class.getName(), null);
            if (refs != null) {
                for (ServiceReference ref : refs) {
                    handleJNDIRegistration(ref, true);
                }
            }
        } catch (InvalidSyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public void serviceChanged(ServiceEvent event) {
        ServiceReference ref = event.getServiceReference();
        switch (event.getType()) {
            case ServiceEvent.REGISTERED:
                handleJNDIRegistration(ref, true);
                break;
            case ServiceEvent.UNREGISTERING:
                handleJNDIRegistration(ref, false);
                break;
        }
    }

    private void handleJNDIRegistration(ServiceReference ref, boolean register) {
        String[] objClasses = (String[]) ref.getProperty(Constants.OBJECTCLASS);
        for (String objClass : objClasses) {
            if (ObjectFactory.class.getName().equals(objClass)) {
                for (String scheme : getStringPlusProperty(ref.getProperty(OSGI_JNDI_URL_SCHEME))) {
                    if (register)
                        InitialContext.addUrlContextFactory(scheme, (ObjectFactory) bundleContext.getService(ref));
                    else
                        InitialContext.removeUrlContextFactory(scheme, (ObjectFactory) bundleContext.getService(ref));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Collection<String> getStringPlusProperty(Object property) {
        if (property instanceof Collection) {
            return (Collection<String>) property;
        } else if (property instanceof String[]) {
            return Arrays.asList((String[]) property);
        } else if (property instanceof String) {
            return Collections.singleton((String) property);
        }
        return Collections.emptyList();
    }
}
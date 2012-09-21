/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.osgi.ejb3.bundle;

import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

import org.jboss.as.test.integration.osgi.api.Echo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.ServiceReference;

/**
 * A simple stateless session bean.
 *
 * @author thomas.diesler@jboss.com
 */
@Stateless
@LocalBean
public class SimpleStatelessSessionBean implements Echo {

    @Resource
    BundleContext context;

    public String echo(String message) {
        if (context == null) {
            throw new IllegalStateException("BundleContext not injected");
        }
        if (BUNDLE_SYMBOLICNAME.equals(message)) {
            Bundle bundle = getTargetBundle();
            return bundle.getSymbolicName();
        } else {
            try {
                Bundle bundle = getTargetBundle();
                bundle.start();
                BundleContext context = bundle.getBundleContext();
                ServiceReference sref = context.getServiceReference(Echo.class.getName());
                Echo service = (Echo) context.getService(sref);
                return service.echo(message);
            } catch (BundleException ex) {
                throw new IllegalStateException("Cannot invoke target service", ex);
            }
        }
    }

    private Bundle getTargetBundle() {
        ClassLoader classLoader = Echo.class.getClassLoader();
        return ((BundleReference) classLoader).getBundle();
    }
}

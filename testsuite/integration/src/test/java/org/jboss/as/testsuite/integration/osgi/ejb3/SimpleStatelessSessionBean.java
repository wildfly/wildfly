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
package org.jboss.as.testsuite.integration.osgi.ejb3;

import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;

import org.jboss.as.testsuite.integration.osgi.xservice.api.Echo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
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

    public String echo(String message) {
        ClassLoader classLoader = Echo.class.getClassLoader();
        Bundle bundle = ((BundleReference) classLoader).getBundle();
        if (BUNDLE_SYMBOLICNAME.equals(message))
            return bundle.getSymbolicName();

        BundleContext context = bundle.getBundleContext();
        ServiceReference sref = context.getServiceReference(Echo.class.getName());
        Echo service = (Echo) context.getService(sref);
        return service.echo(message);
    }
}

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

package org.jboss.as.camel;

import org.apache.camel.CamelContext;

/**
 * An abstraction of {@link CamelContext} registration.
 *
 * The {@link CamelContextRegistry} is the entry point for {@link CamelContext} registration and lookup.
 * The default implementation creates an msc {@link org.jboss.msc.service.Service} as well as an OSGi service.
 *
 * JBoss services can create a dependency on the {@link CamelContext} service like this
 *
 * <code>
        ServiceName serviceName = CamelConstants.CAMEL_CONTEXT_BASE_NAME.append(contextName);
        builder.addDependency(serviceName, CamelContext.class, service.injectedCamelContext);
 * </code>
 *
 * or do an OSGi service lookup like this
 *
 * <code>
        String filter = "(name=" + contextName + ")";
        Collection<ServiceReference<CamelContext>> srefs = context.getServiceReferences(CamelContext.class, filter);
        CamelContext camelctx = context.getService(srefs.iterator().next());
 * </code>
 *
 * @see {@link CamelContextRegistryService}
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Apr-2013
 */
public interface CamelContextRegistry {

    /** Get the camel context for the given name */
    CamelContext getCamelContext(String name);

    /** Register the camel context in this registry */
    CamelContextRegistration registerCamelContext(CamelContext camelContext);

    /** The return handle for camel context registrations */
    interface CamelContextRegistration {
        void unregister();
    }
}

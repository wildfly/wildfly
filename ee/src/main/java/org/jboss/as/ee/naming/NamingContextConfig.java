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

package org.jboss.as.ee.naming;

import org.jboss.msc.service.ServiceName;

/**
 * Configuration item which holds onto the jndi and service name for a module context instance.
 *
 * @author John E. Bailey
 */
public class NamingContextConfig {
    private final ServiceName contextServiceName;

    /**
     * Create a new instance.
     *
     * @param contextServiceName The context service name
     * @param contextName The context jndi name.
     */
    public NamingContextConfig(ServiceName contextServiceName) {
        this.contextServiceName = contextServiceName;
    }

    /**
     * Get the context service name.
     *
     * @return The service name
     */
    public ServiceName getContextServiceName() {
        return contextServiceName;
    }
}

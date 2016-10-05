/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller;


import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceName;

/**
 * Factory for generating a {@link ServiceName} for a binary requirement.
 * @author Paul Ferraro
 */
public interface BinaryServiceNameFactory {
    /**
     * Creates a {@link ServiceName} appropriate for the specified name.
     * @param context an operation context
     * @param parent a parent resource name
     * @param child a child resource name
     * @return a {@link ServiceName}
     */
    ServiceName getServiceName(OperationContext context, String parent, String child);

    /**
     * Creates a {@link ServiceName} appropriate for the specified name.
     * @param support support for capability services
     * @param parent a parent resource name
     * @param child a child resource name
     * @return a {@link ServiceName}
     */
    ServiceName getServiceName(CapabilityServiceSupport support, String parent, String child);
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.spi;

import java.util.Map;

import org.jboss.as.clustering.controller.Capability;
import org.jboss.as.controller.PathAddress;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.service.Requirement;

/**
 * Generic {@link ServiceNameRegistry} for a specific set of requirements.
 * @author Paul Ferraro
 */
public class CapabilityServiceNameRegistry<R extends Requirement, C extends Capability> implements ServiceNameRegistry<R> {

    private final Map<R, C> capabilities;
    private final PathAddress address;

    /**
     * Constructs a new service name registry.
     * @param capabilities a map of requirement to capability
     * @param address the resource address from which to resolve service names
     */
    public CapabilityServiceNameRegistry(Map<R, C> capabilities, PathAddress address) {
        this.capabilities = capabilities;
        this.address = address;
    }

    @Override
    public ServiceName getServiceName(R requirement) {
        return this.capabilities.get(requirement).getServiceName(this.address);
    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.msc.service.ServiceName;

/**
 * Interface to be implemented by capability enumerations.
 * @author Paul Ferraro
 */
public interface Capability extends Definable<RuntimeCapability<?>>, ResourceServiceNameFactory {
    /**
     * Resolves this capability against the specified path address
     * @param address a path address
     * @return a resolved runtime capability
     */
    default RuntimeCapability<?> resolve(PathAddress address) {
        RuntimeCapability<?> definition = this.getDefinition();
        return definition.isDynamicallyNamed() ? definition.fromBaseCapability(address.getLastElement().getValue()) : definition;
    }

    @Override
    default ServiceName getServiceName(PathAddress address) {
        return this.resolve(address).getCapabilityServiceName();
    }
}

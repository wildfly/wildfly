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
 * Provides a factory for generating a {@link ServiceName} for a binary requirement
 * as well as a factory generating a {@link ServiceName} for a unary requirement.
 * @author Paul Ferraro
 */
public interface DefaultableBinaryServiceNameFactoryProvider extends BinaryServiceNameFactoryProvider {

    /**
     * The factory from which to generate a {@link ServiceName} if the requested name is null.
     * @return a factory for generating service names
     */
    UnaryServiceNameFactory getDefaultServiceNameFactory();

    @Override
    default ServiceName getServiceName(OperationContext context, String parent, String child) {
        return (child != null) ? this.getServiceNameFactory().getServiceName(context, parent, child) : this.getDefaultServiceNameFactory().getServiceName(context, parent);
    }

    @Override
    default ServiceName getServiceName(CapabilityServiceSupport support, String parent, String child) {
        return (child != null) ? this.getServiceNameFactory().getServiceName(support, parent, child) : this.getDefaultServiceNameFactory().getServiceName(support, parent);
    }
}

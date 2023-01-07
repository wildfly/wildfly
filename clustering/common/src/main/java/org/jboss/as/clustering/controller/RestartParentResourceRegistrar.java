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

/**
 * Registers a {@link RestartParentResourceAddStepHandler}, {@link RestartParentResourceRemoveStepHandler}, and {@link RestartParentResourceWriteAttributeHandler} on behalf of a resource definition.
 * Users of this class should ensure that the {@link ResourceServiceConfigurator} returned by the specified factory sets the appropriate initial mode of the parent service it configures.
 * Neglecting to do this may result in use of the default initial mode (i.e. ACTIVE), which would result in the parent service starting inadvertently.
 * @author Paul Ferraro
 */
public class RestartParentResourceRegistrar extends ResourceRegistrar {

    public RestartParentResourceRegistrar(ResourceServiceConfiguratorFactory parentFactory, ResourceDescriptor descriptor) {
        this(parentFactory, descriptor, null);
    }

    public RestartParentResourceRegistrar(ResourceServiceConfiguratorFactory parentFactory, ResourceDescriptor descriptor, ResourceServiceHandler handler) {
        super(descriptor, new RestartParentResourceAddStepHandler(parentFactory, descriptor, handler), new RestartParentResourceRemoveStepHandler(parentFactory, descriptor, handler), new RestartParentResourceWriteAttributeHandler(parentFactory, descriptor));
    }
}

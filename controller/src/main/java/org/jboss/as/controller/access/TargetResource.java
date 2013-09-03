/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.access;

import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;

/**
 * The resource that is the target of an action for which access control is needed.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public final class TargetResource {

    private final PathAddress address;
    private final ImmutableManagementResourceRegistration resourceRegistration;
    private final Resource resource;
    private final ServerGroupEffect serverGroupEffect;
    private final HostEffect hostEffect;
    private final List<AccessConstraintDefinition> accessConstraintDefinitions;


    public static TargetResource forStandalone(PathAddress address, ImmutableManagementResourceRegistration resourceRegistration, Resource resource) {
        return new TargetResource(address, resourceRegistration, resource, null, null);
    }

    public static TargetResource forDomain(PathAddress address, ImmutableManagementResourceRegistration resourceRegistration, Resource resource,
                                           ServerGroupEffect serverGroupEffect, HostEffect hostEffect) {
        return new TargetResource(address, resourceRegistration, resource, serverGroupEffect, hostEffect);
    }

    private TargetResource(PathAddress address, ImmutableManagementResourceRegistration resourceRegistration,
                           Resource resource, ServerGroupEffect serverGroupEffect, HostEffect hostEffect) {
        this.address = address;
        this.resourceRegistration = resourceRegistration;
        this.resource = resource;
        this.serverGroupEffect = serverGroupEffect;
        this.hostEffect = hostEffect;
        this.accessConstraintDefinitions = resourceRegistration.getAccessConstraints();
    }

    public PathAddress getResourceAddress() {
        return address;
    }

    public ServerGroupEffect getServerGroupEffect() {
        return serverGroupEffect;
    }

    public HostEffect getHostEffect() {
        return hostEffect;
    }

    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraintDefinitions;
    }

    public Resource getResource() {
        return resource;
    }

    public ImmutableManagementResourceRegistration getResourceRegistration() {
        return resourceRegistration;
    }

}

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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.access.constraint.management.AccessConstraintDefinition;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;

/**
 * The resource that is the target of an action for which access control is needed.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public final class TargetResource {

    private final ImmutableManagementResourceRegistration resourceRegistration;

    private final Resource resource;

    public static TargetResource forStandalone(ImmutableManagementResourceRegistration resourceRegistration, Resource resource) {
        return new TargetResource(resourceRegistration, resource);
    }

    public static TargetResource forDomain(ImmutableManagementResourceRegistration resourceRegistration, Resource resource,
                                           Set<String> serverGroups, Set<String> hosts) {
        return new TargetResource(resourceRegistration, resource);
    }

    private TargetResource(ImmutableManagementResourceRegistration resourceRegistration, Resource resource) {
        this.resourceRegistration = resourceRegistration;
        this.resource = resource;
    }

    public Set<String> getServerGroups() {
        //TODO implement getServerGroups
//        throw new UnsupportedOperationException();
        return Collections.emptySet();
    }

    public Set<String> getHosts() {
        //TODO implement getHosts
//        throw new UnsupportedOperationException();
        return Collections.emptySet();
    }

    public List<AccessConstraintDefinition> getAccessConstraints() {
        return resourceRegistration.getAccessConstraints();
    }

    public Resource getResource() {
        return resource;
    }

    public ImmutableManagementResourceRegistration getResourceRegistration() {
        return resourceRegistration;
    }
}

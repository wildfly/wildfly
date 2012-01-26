/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.host.controller.ignored;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.host.controller.descriptions.HostRootDescription;

/**
 * Registry for excluded domain-level resources. To be used by slave Host Controllers to ignore requests
 * for particular resources that the host cannot understand. This is a mechanism to allow hosts running earlier
 * AS releases to function as slaves in domains whose master is in a later release.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class IgnoredDomainResourceRegistry {

    private final LocalHostControllerInfo localHostControllerInfo;
    private volatile IgnoredDomainResourceRoot rootResource;

    public IgnoredDomainResourceRegistry(LocalHostControllerInfo localHostControllerInfo) {
        this.localHostControllerInfo = localHostControllerInfo;
    }

    /**
     * Returns whether this host should ignore operations from the master domain controller that target
     * the given address.
     *
     * @param address the resource address. Cannot be {@code null}
     *
     * @return {@code true} if the operation should be ignored; {@code false} otherwise
     */
    public boolean isResourceExcluded(final PathAddress address) {
        boolean result = false;
        if (!localHostControllerInfo.isMasterDomainController() && address.size() > 0) {
            IgnoredDomainResourceRoot root = this.rootResource;
            PathElement firstElement = address.getElement(0);
            IgnoreDomainResourceTypeResource typeResource = root == null ? null : root.getChildInternal(firstElement.getKey());
            if (typeResource != null) {
                result = typeResource.hasName(firstElement.getValue());
            }
        }
        return result;
    }

    public void registerResources(final ManagementResourceRegistration parentRegistration) {
        parentRegistration.registerSubModel(new ResourceDefinition());
    }

    public Resource.ResourceEntry getRootResource() {
        IgnoredDomainResourceRoot root = new IgnoredDomainResourceRoot(this);
        this.rootResource = root;
        return root;
    }

    void publish(IgnoredDomainResourceRoot root) {
        this.rootResource = root;
    }

    boolean isMaster() {
        return localHostControllerInfo.isMasterDomainController();
    }

    private class ResourceDefinition extends SimpleResourceDefinition {

        public ResourceDefinition() {
            super(IgnoredDomainResourceRoot.PATH_ELEMENT, HostRootDescription.getResourceDescriptionResolver(ModelDescriptionConstants.IGNORED_RESOURCES));
        }

        @Override
        public void registerChildren(ManagementResourceRegistration resourceRegistration) {
            resourceRegistration.registerSubModel(new IgnoredDomainTypeResourceDefinition());
        }
    }
}

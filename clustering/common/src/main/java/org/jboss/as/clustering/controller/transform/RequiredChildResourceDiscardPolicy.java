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

package org.jboss.as.clustering.controller.transform;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.DiscardPolicy;
import org.jboss.as.controller.transform.description.DynamicDiscardPolicy;
import org.jboss.dmr.ModelNode;

/**
 * Implementation of a generic {@link DynamicDiscardPolicy} that discards child resources which have all their attribute undefined and have no children.
 * Conditionally rejects or leaves resource for further transformation. It is to be used for required child resources that are auto-created.
 *
 * @author Radoslav Husar
 */
public enum RequiredChildResourceDiscardPolicy implements DynamicDiscardPolicy {

    /**
     * Policy that discards if all attributes are undefined and resource has no children; rejects otherwise.
     */
    REJECT_AND_WARN(DiscardPolicy.REJECT_AND_WARN),

    /**
     * Policy that discards if all attributes are undefined and resource has no children;
     * never discards otherwise ({@link DiscardPolicy#NEVER}) in order to proceed with resource transformations.
     */
    NEVER(DiscardPolicy.NEVER),
    ;
    private final DiscardPolicy policy;

    RequiredChildResourceDiscardPolicy(DiscardPolicy policy) {
        this.policy = policy;
    }

    /**
     * @return contextual discard policy if any resource attributes are undefined and has no children; {@link DiscardPolicy#SILENT} otherwise.
     */
    @Override
    public DiscardPolicy checkResource(TransformationContext context, PathAddress address) {
        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        ImmutableManagementResourceRegistration registration = context.getResourceRegistration(PathAddress.EMPTY_ADDRESS);
        ModelNode model = resource.getModel();

        if (model.isDefined()) {
            for (String attribute : registration.getAttributeNames(PathAddress.EMPTY_ADDRESS)) {
                if (model.hasDefined(attribute)) {
                    return this.policy;
                }
            }
        }

        for (PathElement path : registration.getChildAddresses(PathAddress.EMPTY_ADDRESS)) {
            if (path.isWildcard() ? resource.hasChildren(path.getKey()) : resource.hasChild(path)) {
                return this.policy;
            }
        }

        return DiscardPolicy.SILENT;
    }
}

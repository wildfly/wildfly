/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jmx.model;

import javax.management.ObjectName;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.registry.Resource.ResourceEntry;

class RootResourceIterator<T> {
    private final ResourceAccessControlUtil accessControlUtil;
    private final Resource rootResource;
    private final ResourceAction<T> action;

    RootResourceIterator(final ResourceAccessControlUtil accessControlUtil, final Resource rootResource, final ResourceAction<T> action) {
        this.accessControlUtil = accessControlUtil;
        this.rootResource = rootResource;
        this.action = action;
    }

    T iterate() {
        doIterate(rootResource, PathAddress.EMPTY_ADDRESS);
        return action.getResult();
    }

    private void doIterate(final Resource current, final PathAddress address) {
        boolean handleChildren = false;

        ObjectName resourceObjectName = action.onAddress(address);
        if (resourceObjectName != null && accessControlUtil.getResourceAccess(address, false).isAccessibleResource()) {
            handleChildren = action.onResource(resourceObjectName);
        }

        if (handleChildren) {
            for (String type : current.getChildTypes()) {
                if (current.hasChildren(type)) {
                    for (ResourceEntry entry : current.getChildren(type)) {
                        final PathElement pathElement = entry.getPathElement();
                        final Resource child = current.getChild(pathElement);
                        final PathAddress childAddress = address.append(pathElement);
                        doIterate(child, childAddress);
                    }
                }
            }
        }
    }


    interface ResourceAction<T> {
        /**
         * An address has been identified that possibly should be applied to onResource.
         * @param address the address
         * @return an ObjectName representation of the address, or {@code null} if neither the address nor
         *         any of its children are interesting to this ResourceAction.
         */
        ObjectName onAddress(PathAddress address);

        /**
         *
         * @param resourceObjectName the ObjectName returned by onAddress.
         * @return {@code true} if child resources are interesting to this ResourceAction.
         */
        boolean onResource(ObjectName resourceObjectName);

        /**
         * Gets the overall result after all resources have been processed.
         * @return the result
         */
        T getResult();
    }
}

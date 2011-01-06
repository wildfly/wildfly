/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * TODO add class javadoc for UpdateIdentifier
 *
 * @author Brian Stansberry
 *
 */
public class UpdateIdentifier {

    private static final List<PathElement> EMPTY_PATH = Collections.unmodifiableList(new ArrayList<PathElement>(0));

    /**
     * Creates an UpdateIdentifier from the given ModelNode.
     *
     * @param node the node. Cannot be <code>null</code>. Must have a child
     *              named "operation" of type STRING. If has a child named
     *              "address", must of of type LIST with each child in the
     *              list of type PROPERTY
     *
     * @return the update identifer
     */
    public static UpdateIdentifier parse(final ModelNode node) {

        final ModelNode op = node.get("operation");
        if (!op.isDefined()) {
            throw new IllegalArgumentException("Child 'operation' is undefined");
        }
        List<PathElement> path = null;
        final ModelNode address = node.get("address");
        if (address.isDefined()) {
            final List<Property> props = address.asPropertyList();
            if (props.size() == 0) {
                path = EMPTY_PATH;
            }
            else {
                path = new ArrayList<PathElement>(props.size());
                for (final Property prop : props) {
                    path.add(new PathElement(prop.getName(), prop.getValue().asString()));
                }
            }
        }
        else {
            path = EMPTY_PATH;
        }

        return new UpdateIdentifier(path, op.asString());
    }

    private final List<PathElement> addressType;
    private final String updateId;

    public UpdateIdentifier(final List<PathElement> addressType, final String updateId) {
        assert addressType != null : "addressType is null";
        assert updateId != null : "updateId is null";
        this.addressType = addressType;
        this.updateId = updateId;
    }

    public List<PathElement> getAddressType() {
        return addressType;
    }

    public String getUpdateId() {
        return updateId;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result += 31 * addressType.hashCode();
        result += 31 * updateId.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;

        if (obj instanceof UpdateIdentifier) {
            final UpdateIdentifier other = (UpdateIdentifier) obj;
            return addressType.equals(other.addressType) && updateId.equals(other.updateId);
        }
        return false;
    }

}

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

package org.jboss.as.controller.operations.global;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FILTERED_ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FILTERED_CHILDREN_TYPES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNREADABLE_CHILDREN;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Collection point for information about data filtered from a :read-resource[-description] call.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
class FilteredData {

    private final int baseAddressLength;
    private Map<PathAddress, ResourceData> map;

    FilteredData(PathAddress baseAddress) {
        this.baseAddressLength = baseAddress.size();
    }

    void addReadRestrictedAttribute(PathAddress fullAddress, String attribute) {
        ResourceData rd = getResourceData(fullAddress);
        if (rd.attributes == null) {
            rd.attributes = new HashSet<String>();
        }
        rd.attributes.add(attribute);
    }

    void addReadRestrictedResource(PathAddress fullAddress) {
        assert fullAddress.size() > 0 : "cannot filter root resource";
        ResourceData rd = getResourceData(fullAddress.subAddress(0, fullAddress.size() - 1));
        if (rd.children == null) {
            rd.children = new HashSet<PathElement>();
        }
        rd.children.add(fullAddress.getLastElement());

    }

    void addAccessRestrictedResource(PathAddress fullAddress) {
        assert fullAddress.size() > 0 : "cannot filter root resource";
        ResourceData rd = getResourceData(fullAddress.subAddress(0, fullAddress.size() - 1));
        if (rd.childTypes == null) {
            rd.childTypes = new HashSet<PathElement>();
        }
        rd.childTypes.add(fullAddress.getLastElement());

    }

    boolean hasFilteredData() {
        return map != null;
    }

    boolean isFilteredResource(PathAddress parent, PathElement child) {
        boolean result = false;
        ResourceData rd = map == null ? null : map.get(parent);
        if (rd != null) {
            result = (rd.children != null && rd.children.contains(child))
                       || (rd.childTypes != null && rd.childTypes.contains(child));
        }
        return result;
    }

    boolean isAddressFiltered(PathAddress parent, PathElement child) {
        boolean result = false;
        ResourceData rd = map == null ? null : map.get(parent);
        if (rd != null) {
            result = (rd.childTypes != null && rd.childTypes.contains(child));
        }
        return result;
    }

    ModelNode toModelNode() {
        ModelNode result = null;
        if (map != null) {
            result = new ModelNode();
            for (Map.Entry<PathAddress, ResourceData> entry : map.entrySet()) {
                ModelNode item = new ModelNode();
                PathAddress pa = entry.getKey();
                item.get("absolute-address").set(pa.toModelNode());
                ResourceData rd = entry.getValue();
                item.get("relative-address").set(pa.subAddress(baseAddressLength).toModelNode());
                ModelNode attrs = new ModelNode().setEmptyList();
                if (rd.attributes != null) {
                    for (String attr : rd.attributes) {
                        attrs.add(attr);
                    }
                }
                if (attrs.asInt() > 0) {
                    item.get(FILTERED_ATTRIBUTES).set(attrs);
                }
                ModelNode children = new ModelNode().setEmptyList();
                if (rd.children != null) {
                    for (PathElement pe : rd.children) {
                        children.add(new Property(pe.getKey(), new ModelNode(pe.getValue())));
                    }
                }
                if (children.asInt() > 0) {
                    item.get(UNREADABLE_CHILDREN).set(children);
                }
                ModelNode childTypes = new ModelNode().setEmptyList();
                if (rd.childTypes != null) {
                    Set<String> added = new HashSet<String>();
                    for (PathElement pe : rd.childTypes) {
                        if (added.add(pe.getKey())) {
                            childTypes.add(pe.getKey());
                        }
                    }
                }
                if (childTypes.asInt() > 0) {
                    item.get(FILTERED_CHILDREN_TYPES).set(childTypes);
                }
                result.add(item);
            }
        }
        return result;
    }

    private ResourceData getResourceData(PathAddress fullAddress) {
        if (map == null) {
            map = new LinkedHashMap<PathAddress, ResourceData>();
        }
        ResourceData result = map.get(fullAddress);
        if (result == null) {
            result = new ResourceData();
            map.put(fullAddress, result);
        }
        return result;
    }

    private static class ResourceData {
        private Set<String> attributes;
        private Set<PathElement> children;
        private Set<PathElement> childTypes;
    }
}

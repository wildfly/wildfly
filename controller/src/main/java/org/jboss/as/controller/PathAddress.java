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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * A path address for an operation.
 *
 * @author Brian Stansberry
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class PathAddress implements Iterable<PathElement> {

    private static final PathAddress EMPTY_ADDRESS = new PathAddress(Collections.<PathElement>emptyList());

    /**
     * Creates an UpdateIdentifier from the given ModelNode address.  The given node is expected
     * to be an address node.
     *
     * @param node the node (cannot be {@code null})
     *
     * @return the update identifier
     */
    public static PathAddress pathAddress(final ModelNode node) {
        final Map<String, PathElement> pathMap;
        if (node.isDefined()) {
            final List<Property> props = node.asPropertyList();
            if (props.size() == 0) {
                return EMPTY_ADDRESS;
            } else {
                pathMap = new LinkedHashMap<String, PathElement>();
                for (final Property prop : props) {
                    final String name = prop.getName();
                    if (pathMap.put(name, new PathElement(name, prop.getValue().asString())) != null) {
                        throw duplicateElement(name);
                    }
                }
            }
        } else {
            return EMPTY_ADDRESS;
        }
        return new PathAddress(Collections.unmodifiableList(new ArrayList<PathElement>(pathMap.values())));
    }

    public static PathAddress pathAddress(List<PathElement> elements) {
        if (elements.size() == 0) {
            return EMPTY_ADDRESS;
        }
        final ArrayList<PathElement> newList = new ArrayList<PathElement>(elements.size());
        final Set<String> seen = new HashSet<String>();
        for (PathElement element : elements) {
            final String name = element.getKey();
            if (seen.add(name)) {
                newList.add(element);
            } else {
                throw duplicateElement(name);
            }
        }
        return new PathAddress(Collections.unmodifiableList(newList));
    }

    public static PathAddress pathAddress(PathElement... elements) {
        return pathAddress(Arrays.<PathElement>asList(elements));
    }

    private static IllegalArgumentException duplicateElement(final String name) {
        return new IllegalArgumentException("Duplicate path element \"" + name + "\" found");
    }

    private final List<PathElement> pathAddressList;

    PathAddress(final List<PathElement> pathAddressList) {
        assert pathAddressList != null : "pathAddressList is null";
        this.pathAddressList = pathAddressList;
    }

    public ListIterator<PathElement> iterator() {
        return pathAddressList.listIterator();
    }

    @Override
    public int hashCode() {
        int result = 17;
        result += 31 * pathAddressList.hashCode();
        return result;
    }

    /**
     * Determine whether this object is equal to another.
     *
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(Object other) {
        return other instanceof PathAddress && equals((PathAddress)other);
    }

    /**
     * Determine whether this object is equal to another.
     *
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(PathAddress other) {
        return this == other || other != null && pathAddressList.equals(other.pathAddressList);
    }
}

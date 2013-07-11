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
package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.clustering.jgroups.subsystem.ModelKeys.PROTOCOL;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Custom resource to maintain protocol order.
 *
 * A wrapper for a standard resource which does the following:
 * - an internal List is used to record order
 * - when registering or removing a protocol child update the list
 * - when returning the set of protocol children, use the list to create a SortedSet
 * which places a total order on the Set of protocol children
 * - for any other operation, pass to delegate
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class StackResource implements Resource {

    private final Resource delegate;
    private volatile List<String> protocols;

    public StackResource() {
        this(Resource.Factory.create());
    }

    public StackResource(final Resource delegate) {
        this.delegate = delegate;
        // initialise the list of protocol order
        this.protocols = new ArrayList<String>();
    }

    // get the ordered list of protocol names maintained by this custom resource
    public List<String> getProtocols() {
        return protocols;
    }

    public void setProtocols(List<String> protocols) {
        this.protocols = protocols;
    }

    @Override
    public ModelNode getModel() {
        return delegate.getModel();
    }

    @Override
    public void writeModel(ModelNode newModel) {
        delegate.writeModel(newModel);
    }

    @Override
    public boolean isModelDefined() {
        return delegate.isModelDefined();
    }

    @Override
    public boolean hasChild(PathElement element) {
        return delegate.hasChild(element);
    }

    @Override
    public Resource getChild(PathElement element) {
        return delegate.getChild(element);
    }

    @Override
    public Resource requireChild(PathElement element) {
        return delegate.requireChild(element);
    }

    @Override
    public boolean hasChildren(String childType) {
        return delegate.hasChildren(childType);
    }

    @Override
    public Resource navigate(PathAddress address) {
        return delegate.navigate(address);
    }

    @Override
    public Set<String> getChildTypes() {
        return delegate.getChildTypes();
    }

    /*
     * When returning children, make sure they are ordered.
     */
    @Override
    public Set<String> getChildrenNames(String childType) {
        if (PROTOCOL.equals(childType)) {
            // return a set created from the ordered list
            ArrayList<String> clone = new ArrayList<String>(getProtocols());
            Comparator<String> comparator = new ProtocolStringComparator(clone);
            TreeSet<String> resultSet = new TreeSet<String>(comparator);
            resultSet.addAll(delegate.getChildrenNames(childType));
            return resultSet;
        } else {
            return delegate.getChildrenNames(childType);
        }
    }

    /*
     * We have to add in the children here!
     */
    @Override
    public Set<ResourceEntry> getChildren(String childType) {
        if (PROTOCOL.equals(childType)) {
            // return a set created from the ordered list
            ArrayList<String> clone = new ArrayList<String>(getProtocols());
            Comparator<ResourceEntry> comparator = new ProtocolResourceEntryComparator(clone);
            TreeSet<ResourceEntry> resultSet = new TreeSet<ResourceEntry>(comparator);
            resultSet.addAll(delegate.getChildren(childType));
            return resultSet;
        } else {
            return delegate.getChildren(childType);
        }
    }

    /*
     * When adding a protocol child, make note of its order and store in the list.
     */
    @Override
    public void registerChild(PathElement address, Resource resource) {
        if (PROTOCOL.equals(address.getKey())) {
            // add the protocol name to the ordered list
            protocols.add(address.getValue());
            delegate.registerChild(address, resource);
        } else {
            delegate.registerChild(address, resource);
        }
    }

    /*
     * When removing a protocol child, remove it from the list.
     */
    @Override
    public Resource removeChild(PathElement address) {
        if (PROTOCOL.equals(address.getKey())) {
            // remove the protocol name from the ordered list
            protocols.remove(protocols.indexOf(address.getValue()));
            return delegate.removeChild(address);
        } else {
            return delegate.removeChild(address);
        }
    }

    @Override
    public boolean isRuntime() {
        return delegate.isRuntime();
    }

    @Override
    public boolean isProxy() {
        return delegate.isProxy();
    }

    @Override
    public Resource clone() {
        StackResource clone = new StackResource(delegate.clone());
        clone.setProtocols(getProtocols());
        return clone;
    }

    /*
     * Comparator for a set to determine element order.
     */
    private class ProtocolStringComparator implements Comparator<String> {
        private List<String> order = null;

        private ProtocolStringComparator(final List order) {
            this.order = order;
        }

        @Override
        public int compare(String o1, String o2) {
            // the parameters should always be in the list
            // what if they are not?
            int index1 = order.indexOf(o1);
            int index2 = order.indexOf(o2);

            if (index1 < index2) {
                return -1;
            } else if (index1 > index2) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    /*
     * Comparator for a set to determine element order.
     */
    private class ProtocolResourceEntryComparator implements Comparator<ResourceEntry> {
        private List<String> order = null;

        private ProtocolResourceEntryComparator(final List order) {
            this.order = order;
        }

        @Override
        public int compare(ResourceEntry re1, ResourceEntry re2) {
            // the parameters should always be in the list
            // what if they are not?
            int index1 = order.indexOf(re1.getName());
            int index2 = order.indexOf(re2.getName());

            if (index1 < index2) {
                return -1;
            } else if (index1 > index2) {
                return 1;
            } else {
                return 0;
            }
        }
    }

}

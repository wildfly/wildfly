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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jgroups.Channel;
import org.jgroups.stack.Protocol;

/**
 * Custom resource to allow dynamic detection, registration and return of protocol resources.
 *
 * This custom resource needs to do the following:
 * - obtain a reference to the channel to obtain the (static) list of protocols in the channel's stack
 * - register the protocols as runtime-only resources
 * - return the correct set of protocol child resources when children are interrogated
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class ChannelInstanceResource implements Resource {

    public static final ServiceName CHANNEL_PARENT = ServiceName.of("jboss", "jgroups", "channel");
    public static final int CHANNEL_PREFIX_LENGTH = CHANNEL_PARENT.toString().length();
    public static final String JGROUPS_PROTOCOL_PKG = "org.jgroups.protocols";
    public static final String JGROUPS_PROTOCOL_PACKAGE = "package org.jgroups.protocols";
    // disregard this Protocol which is used in a shared transport configuration
    private static final String SHARED_TRANSPORT_PROTOCOL = "TP.ProtocolAdapter";

    final ServiceController<Channel> controller;

    public ChannelInstanceResource(final ServiceController<Channel> controller) {
        this.controller = controller;
    }

    // this resource holds no persistent state, so "turn off" the model
    @Override
    public ModelNode getModel() {
        return new ModelNode();
    }

    @Override
    public void writeModel(ModelNode newModel) {
        throw ControllerLogger.ROOT_LOGGER.immutableResource();
    }

    @Override
    public boolean isModelDefined() {
        return false;
    }

    // this resource does have children, so activate te creation of children
    // the children are protocol resources associated with the channel
    @Override
    public boolean hasChild(PathElement element) {
        if (ModelKeys.PROTOCOL.equals(element.getKey())) {
            return hasProtocol(element);
        }
        return false;
    }

    @Override
    public Resource requireChild(PathElement element) {
        if (ModelKeys.PROTOCOL.equals(element.getKey())) {
            if (hasProtocol(element)) {
                return PlaceholderResource.INSTANCE;
            }
        }
        throw new NoSuchResourceException(element);
    }

    @Override
    public Resource getChild(PathElement element) {
        return hasProtocol(element) ? PlaceholderResource.INSTANCE : null;
    }

    @Override
    public Resource removeChild(PathElement address) {
        throw ControllerLogger.ROOT_LOGGER.immutableResource();
    }

    @Override
    public void registerChild(PathElement address, Resource resource) {
        throw ControllerLogger.ROOT_LOGGER.immutableResource();
    }

    @Override
    public boolean hasChildren(String childType) {
        if (ModelKeys.PROTOCOL.equals(childType)) {
            return getChildrenNames(ModelKeys.PROTOCOL).size() > 0;
        } else {
            return false;
        }
    }

    @Override
    public Set<ResourceEntry> getChildren(String childType) {
        if (ModelKeys.PROTOCOL.equals(childType)) {
            Set<ResourceEntry> result = new HashSet<ResourceEntry>();
            for (String name : getProtocolNames()) {
                result.add(new PlaceholderResource.PlaceholderResourceEntry(ModelKeys.PROTOCOL, name));
            }
            return result;
        }
        return Collections.emptySet();
    }

    @Override
    public Set<String> getChildrenNames(String childType) {
        if (ModelKeys.PROTOCOL.equals(childType)) {
            return getProtocolNames();
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public Set<String> getChildTypes() {
        Set<String> result = new HashSet<String>();
        result.add(ModelKeys.PROTOCOL);
        return result;
    }

    @Override
    public Resource navigate(PathAddress address) {
        // TODO: check this
        return Resource.Tools.navigate(this, address);
    }

    @Override
    public Resource clone() {
        // don't clone the pointer to the unique controller for this service
        return new ChannelInstanceResource(this.controller);
    }

    @Override
    public boolean isRuntime() {
        return true;
    }

    @Override
    public boolean isProxy() {
        return false;
    }

    private boolean hasProtocol(PathElement element) {
        String protocolName = element.getValue();
        return getProtocolNames().contains(protocolName);
    }

    private Set<String> getProtocolNames() {
        Channel channel = this.controller.getValue();
        if (channel == null) {
            return Collections.emptySet();
        }
        Set<String> names = new HashSet<String>();
        List<Protocol> protocols = channel.getProtocolStack().getProtocols();
        for (Protocol protocol : protocols) {
            String fullyQualifiedProtocolName = protocol.getClass().getPackage() + "." + protocol.getName();
            String protocolName = fullyQualifiedProtocolName.substring(JGROUPS_PROTOCOL_PACKAGE.length()+1);
            // disregard the shared transport protocol adapter
            if (!protocolName.equals(SHARED_TRANSPORT_PROTOCOL)) {
                names.add(protocolName);
            }
        }
        return names ;
    }

    /*
     * ResourceEntry extends the resource and additionally provides information on its path
     */
    public static class ChannelInstanceResourceEntry extends ChannelInstanceResource implements ResourceEntry {

        final PathElement path;

        public ChannelInstanceResourceEntry(final ServiceController<Channel> controller, final PathElement path) {
            super(controller);
            this.path = path;
        }

        public ChannelInstanceResourceEntry(final ServiceController<Channel> controller, final String type, final String name) {
            super(controller);
            this.path = PathElement.pathElement(type, name);
        }

        @Override
        public String getName() {
            return path.getValue();
        }

        @Override
        public PathElement getPathElement() {
            return path;
        }

        @Override
        public ChannelInstanceResourceEntry clone() {
            return new ChannelInstanceResourceEntry(this.controller, getPathElement());
        }
    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.model.socket.InterfaceAdd;
import org.jboss.as.model.socket.InterfaceElement;
import org.jboss.as.model.socket.SocketBindingAdd;
import org.jboss.as.model.socket.SocketBindingElement;
import org.jboss.as.model.socket.SocketBindingGroupElement;
import org.jboss.as.model.socket.SocketBindingGroupUpdate;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServerFactory {

    private ServerFactory() {
    }

    /**
     * Combine a domain model and a host model to generate a list of bootstrap updates for a server to run.
     *
     * @param domainModel the domain model
     * @param hostModel the host model
     * @param serverName the name of the server to bootstrap
     * @param list the list to which the updates should be appended
     */
    @SuppressWarnings({ "RawUseOfParameterizedType" })
    public static void combine(DomainModel domainModel, HostModel hostModel, String serverName, List<AbstractServerModelUpdate<?>> list) {
        // Validate the model
        final ServerElement serverElement = hostModel.getServer(serverName);
        if (serverElement == null) {
            throw new IllegalArgumentException("Host model does not contain a server named '" + serverName + "'");
        }
        final String serverGroupName = serverElement.getServerGroup();
        final ServerGroupElement serverGroup = domainModel.getServerGroup(serverGroupName);
        if (serverGroup == null) {
            throw new IllegalArgumentException("Domain model does not contain a server group named '" + serverGroupName + "'");
        }
        final String profileName = serverGroup.getProfileName();
        final ProfileElement leafProfile = domainModel.getProfile(profileName);
        if (profileName == null) {
            throw new IllegalArgumentException("Domain model does not contain a profile named '" + profileName + "'");
        }

        list.add(new ServerNameUpdate(serverName));

        // Merge extensions
        final Set<String> extensionNames = new LinkedHashSet<String>();
        for (String name : domainModel.getExtensions()) {
            extensionNames.add(name);
        }
        for (String name : hostModel.getExtensions()) {
            extensionNames.add(name);
        }
        for (String name : extensionNames) {
            list.add(new ServerExtensionAdd(name));
        }

        // Merge paths
        final Set<String> unspecifiedPaths = new HashSet<String>();
        final Map<String, ServerPathAdd> paths = new HashMap<String, ServerPathAdd>();
        for(final PathElement path : domainModel.getPaths()) {
            if(! path.isSpecified()) {
                unspecifiedPaths.add(path.getName());
            } else {
                paths.put(path.getName(), new ServerPathAdd(path));
            }
        }
        for(final PathElement path : hostModel.getPaths()) {
            unspecifiedPaths.remove(path.getName());
            paths.put(path.getName(), new ServerPathAdd(path));
        }
        for(final PathElement path : serverElement.getPaths()) {
            unspecifiedPaths.remove(path.getName());
            paths.put(path.getName(), new ServerPathAdd(path));
        }
        if(unspecifiedPaths.size() > 0) {
            throw new IllegalStateException("unspecified paths " + unspecifiedPaths);
        }

        // Merge interfaces
        // TODO: modify to merge each interface instead of replacing duplicates
        Set<String> unspecifiedInterfaces = new HashSet<String>();
        Map<String, InterfaceElement> interfaces = new HashMap<String, InterfaceElement>();
        for (InterfaceElement ie : domainModel.getInterfaces()) {
            if (ie.isFullySpecified()) {
                interfaces.put(ie.getName(), ie);
            } else {
                unspecifiedInterfaces.add(ie.getName());
            }
        }
        for (InterfaceElement ie : hostModel.getInterfaces()) {
            interfaces.put(ie.getName(), ie);
            unspecifiedInterfaces.remove(ie.getName());
        }
        for (InterfaceElement ie : serverElement.getInterfaces()) {
            interfaces.put(ie.getName(), ie);
            unspecifiedInterfaces.remove(ie.getName());
        }
        // TODO: verify that all required interfaces were specified

        for (InterfaceElement interfaceElement : interfaces.values()) {
            list.add(new ServerModelInterfaceAdd(new InterfaceAdd(interfaceElement)));
        }

        // Merge socket bindings
        String bindingRef = serverElement.getSocketBindingGroupName();
        int portOffset = serverElement.getSocketBindingPortOffset();
        if (bindingRef == null) {
            bindingRef = serverGroup.getSocketBindingGroupName();
            portOffset = serverGroup.getSocketBindingPortOffset();
        }
        list.add(new ServerPortOffsetUpdate(portOffset));

        // TODO: add check for duplicate socket bindings
        SocketBindingGroupElement domainBindings = domainModel.getSocketBindingGroup(bindingRef);
        if (domainBindings == null) {
            domainBindings = new SocketBindingGroupElement("domainBindings");
        }
        list.add(new ServerSocketBindingGroupUpdate(new SocketBindingGroupUpdate(domainBindings.getName(), domainBindings.getDefaultInterface(), Collections.<String>emptySet())));
        processSocketBindings(domainBindings, list);
        for(final String socketInclude : domainBindings.getIncludedSocketBindingGroups()) {
            final SocketBindingGroupElement include = domainModel.getSocketBindingGroup(socketInclude);
            if(include == null) {
                throw new IllegalStateException("failed to resolve binding-group " + socketInclude);
            }
            processSocketBindings(include, list);
        }

        list.add(new ServerProfileUpdate(serverGroup.getProfileName()));
        // Merge subsystems
        for (AbstractSubsystemElement<? extends AbstractSubsystemElement<?>> subsystemElement : leafProfile.getSubsystems()) {
            // todo: find a better way around this generics issue
            processSubsystem((AbstractSubsystemElement) subsystemElement, list);
        }

        // Merge deployments
        for (ServerGroupDeploymentElement element : serverGroup.getDeployments()) {
            final ServerModelDeploymentAdd add = new ServerModelDeploymentAdd(element.getUniqueName(), element.getRuntimeName(), element.getSha1Hash(), element.isStart());
            list.add(add);
        }

        // Merge system properties
        // todo after PropertiesElement exposes flags as to whether individual properties
        // are passed to java.lang.Process or go through the model

        // TODO add domain deployment repository
        // BES 2010/10/12 -- why?

    }

    private static void processSocketBindings(final SocketBindingGroupElement group, List<AbstractServerModelUpdate<?>> list) {
        for(final SocketBindingElement binding : group.getSocketBindings()) {
            final SocketBindingAdd update = new SocketBindingAdd(binding);
            list.add(new ServerSocketBindingUpdate(update));
        }

    }

    private static <E extends AbstractSubsystemElement<E>> void processSubsystem(E subsystemElement, List<AbstractServerModelUpdate<?>> list) {
        final AbstractSubsystemAdd<E> subsystemAdd = subsystemElement.getAdd();
        if (subsystemAdd == null) {
            throw new IllegalStateException(subsystemElement + " did not provide an " + AbstractSubsystemAdd.class.getSimpleName());
        }
        list.add(new ServerSubsystemAdd(subsystemAdd));
        final List<AbstractSubsystemUpdate<E, ?>> subsystemList = new ArrayList<AbstractSubsystemUpdate<E,?>>();
        subsystemElement.getUpdates(subsystemList);
        for (AbstractSubsystemUpdate<E, ?> update : subsystemList) {
            list.add(ServerSubsystemUpdate.create(update));
        }
    }
}

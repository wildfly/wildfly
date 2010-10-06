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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.model.socket.InterfaceAdd;
import org.jboss.as.model.socket.InterfaceElement;
import org.jboss.as.model.socket.SocketBindingGroupElement;
import org.jboss.as.model.socket.SocketBindingGroupRefElement;

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

        // Merge interfaces
        // TODO: modify to merge each interface instead of replacing duplicates
        Set<String> unspecifiedInterfaces = new HashSet<String>();
        Map<String, InterfaceElement> interfaces = new HashMap<String, InterfaceElement>();
        for (InterfaceElement ie : domainModel.getInterfaces()) {
            if (ie.isFullySpecified())
                interfaces.put(ie.getName(), ie);
            else
                unspecifiedInterfaces.add(ie.getName());
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
        SocketBindingGroupRefElement bindingRef = serverElement.getSocketBindingGroup();
        if (bindingRef == null) {
            bindingRef = serverGroup.getSocketBindingGroup();
        }
        SocketBindingGroupElement domainBindings = domainModel.getSocketBindingGroup(bindingRef.getRef());
        if (domainBindings == null) {
            domainBindings = new SocketBindingGroupElement("domainBindings");
        }
        int portOffset = bindingRef.getPortOffset();
        // TODO: add each socket binding

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
        // todo after PropertiesElement is eliminated
    }

    private static <E extends AbstractSubsystemElement<E>> void processSubsystem(E subsystemElement, List<AbstractServerModelUpdate<?>> list) {
        final AbstractSubsystemAdd<E> subsystemAdd = subsystemElement.getAdd();
        list.add(new ServerSubsystemAdd(subsystemAdd));
        final List<AbstractSubsystemUpdate<E, ?>> subsystemList = new ArrayList<AbstractSubsystemUpdate<E,?>>();
        subsystemElement.getUpdates(subsystemList);
        for (AbstractSubsystemUpdate<E, ?> update : subsystemList) {
            list.add(ServerSubsystemUpdate.create(update));
        }
    }
}

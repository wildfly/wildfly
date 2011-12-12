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

package org.jboss.as.controller.registry;

import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;

/**
 * Read-only view of a {@link ManagementResourceRegistration}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface ImmutableManagementResourceRegistration {

    /**
     * Gets whether this model node only exists in the runtime and has no representation in the
     * persistent configuration model.
     *
     * @return {@code true} if the model node has no representation in the
     * persistent configuration model; {@code false} otherwise
     */
    boolean isRuntimeOnly();

    /**
     * Gets whether operations against the resource represented by this registration will be proxied to
     * a remote process.
     *
     * @return {@code true} if this registration represents a remote resource; {@code false} otherwise
     */
    boolean isRemote();

    /**
     * Get the operation handler at the given address, or {@code null} if none exists.
     *
     * @param address the address, relative to this node
     * @param operationName the operation name
     * @return the operation handler
     */
    OperationStepHandler getOperationHandler(PathAddress address, String operationName);

    /**
     * Get the operation description at the given address, or {@code null} if none exists.
     *
     * @param address the address, relative to this node
     * @param operationName the operation name
     * @return the operation description
     */
    DescriptionProvider getOperationDescription(PathAddress address, String operationName);

    /**
     * Get the special characteristic flags for the operation at the given address, or {@code null} if none exist.
     *
     * @param address the address, relative to this node
     * @param operationName the operation name
     * @return the operation entry flags or {@code null}
     *
     */
    Set<OperationEntry.Flag> getOperationFlags(PathAddress address, String operationName);

    /**
     * Get the entry representing an operation registered with the given name at the given address, or {@code null} if none exists.
     *
     * @param address the address, relative to this node
     * @param operationName the operation name
     * @return the operation entry or {@code null}
     *
     */
    OperationEntry getOperationEntry(PathAddress address, String operationName);

    /**
     * Get the names of the attributes for a node
     *
     * @param address the address, relative to this node
     * @return the attribute names. If there are none an empty set is returned
     */
    Set<String> getAttributeNames(PathAddress address);

    /**
     * Gets the information on how to read from or write to the given attribute.
     *
     * @param address the address of the resource
     * @param attributeName the name of the attribute
     *
     * @return the handling information, or {@code null} if the attribute or address is unknown
     */
    AttributeAccess getAttributeAccess(PathAddress address, String attributeName);

    /**
     * Get the names of the operations for a node
     *
     * @param address the address, relative to this node
     * @return the operation names. If there are none an empty set is returned
     */
    Set<String> getChildNames(PathAddress address);

    /**
     * Gets the set of direct child address elements under the node at the passed in PathAddress
     *
     * @param address the address we want to find children for
     * @return the set of direct child elements
     */
    Set<PathElement> getChildAddresses(PathAddress address);

    /**
     * Get the model description at the given address, or {@code null} if none exists.
     *
     * @param address the address, relative to this node
     * @return the model description
     */
    DescriptionProvider getModelDescription(PathAddress address);

    /**
     * Get a map of descriptions of all operations available at an address.
     *
     * @param address the address
     * @param inherited true to include inherited operations
     * @return the operation map
     */
    Map<String, OperationEntry> getOperationDescriptions(PathAddress address, boolean inherited);

    /**
     * If there is a proxy controller registered under any part of the registered address it will be returned.
     * E.g. if the address passed in is <code>[a=b,c=d,e=f]</code> and there is a proxy registered under
     * <code>[a=b,c=d]</code> that proxy will be returned.
     *
     * @param address the address to look for a proxy under
     * @return the found proxy controller, or <code>null</code> if there is none
     */
    ProxyController getProxyController(PathAddress address);

    /**
     * Finds all proxy controllers registered at the passed in address, or at lower levels.
     * <p/>
     * E.g. if the address passed in is <code>a=b</code> and there are proxies registered at
     * <code>[a=b,c=d]</code>, <code>[a=b,e=f]</code> and <code>[g-h]</code>, the proxies for
     * <code>[a=b,c=d]</code> and <code>[a=b,e=f]</code> will be returned.
     *
     * @param address the address to start looking for proxies under
     * @return the found proxy controllers, or an empty set if there are none
     */
    Set<ProxyController> getProxyControllers(PathAddress address);

    /**
     * Get a sub model registration.
     *
     * @param address the address, relative to this node
     * @return the node registration, <code>null</code> if there is none
     */
    ImmutableManagementResourceRegistration getSubModel(PathAddress address);
}

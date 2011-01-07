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

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface SubsystemRegistration {

    /**
     * Set the parser for the profile-wide subsystem configuration XML element.  The element is always
     * called {@code "subsystem"}.  The reader should populate the given model node with the appropriate
     * "subsystem add" update, without the address or operation name as that information will be automatically
     * populated.
     *
     * @param reader the element reader
     */
    void setSubsystemParser(XMLElementReader<ModelNode> reader);

    /**
     * Set the parser for the per-deployment configuration for this element, if any.
     *
     * (TODO: round this out.)
     *
     * @param reader the element reader
     */
    void setDeploymentParser(XMLElementReader<ModelNode> reader);

    /**
     * Register a subsystem operation handler.  The "add, "remove", and "read" handlers are automatically registered
     * and cannot be overridden.  The given address is relative, so to register an operation which runs on the root
     * of the subsystem, use {@link PathAddress#EMPTY_ADDRESS}.
     *
     * @param operationName the operation name (must not be {@code null})
     * @param relativeAddress the relative address (must not be {@code null})
     * @param handler the handler (must not be {@code null})
     * @throws IllegalArgumentException if the operation name, address, or handler is {@code null} or invalid, or if
     * the operation name is already registered at that address
     */
    void registerOperationHandler(String operationName, PathAddress relativeAddress, OperationHandler handler) throws IllegalArgumentException;

    /**
     * TODO: a per-deployment operation handler for add/remove/modify of per-deployment overrides
     *
     * @param operationName
     * @param relativeAddress
     * @param handler
     * @throws IllegalArgumentException
     */
    void registerDeploymentOperationHandler(String operationName, PathAddress relativeAddress, OperationHandler handler) throws IllegalArgumentException;
}

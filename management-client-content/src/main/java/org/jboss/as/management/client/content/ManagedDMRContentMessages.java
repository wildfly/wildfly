/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.management.client.content;

import java.security.NoSuchAlgorithmException;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;

/**
 * This module is using message IDs in the range 12200-12299. This file is using the subset 12250-12299 for
 * non-logger messages. See http://community.jboss.org/docs/DOC-16810 for the full list of currently reserved
 * JBAS message id blocks.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@MessageBundle(projectCode = "JBAS")
public interface ManagedDMRContentMessages {

    /**
     * The messages.
     */
    ManagedDMRContentMessages MESSAGES = Messages.getBundle(ManagedDMRContentMessages.class);

    /**
     * Creates an exception indicating the expected content hash provided by the caller does not match the current value.
     *
     * @param expectedHash the expected hash of the content
     * @param address the address of the content
     * @param currentHash the current hash of the content
     *
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 12250, value = "Invalid hash '%s' for content at address %s; current hash is '%s' -- perhaps the content has been updated by another caller?")
    OperationFailedException invalidHash(String expectedHash, PathAddress address, String currentHash);

    /**
     * Creates a runtime exception indicating the SHA-1 message digest algorithm is not available.
     *
     * @param cause the cause.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 12251, value = "Cannot obtain Message Digest algorithm SHA-1")
    IllegalStateException messageDigestAlgorithmNotAvailable(@Cause NoSuchAlgorithmException cause);

    /**
     * Creates a runtime exception indicating the type element of resource being registered is incorrect.
     *
     * @param type the provided type.
     * @param legalType the valid type
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 12252, value = "Illegal child type %s -- must be %s")
    IllegalArgumentException illegalChildType(String type, String legalType);

    /**
     * Creates a runtime exception indicating an attempt was made to register a resource whose implementation is
     * not the correct class. This would indicate a programming error, not a user error,
     *
     * @param clazz the class of the resource that cannot be registered
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 12253, value = "Illegal child resource class %s")
    IllegalArgumentException illegalChildClass(Class<? extends Resource> clazz);

    /**
     * Creates a runtime exception indicating no content could be found using the given hash
     *
     * @param hash the hash of the content
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 12254, value = "No content found with hash %s")
    IllegalStateException noContentFoundWithHash(String hash);

    /**
     * Creates a runtime exception indicating a ManagedDMRContentResource did not have a parent resource
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 12255, value = "null parent")
    IllegalStateException nullParent();
}

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

package org.jboss.as.server;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.services.security.VaultReaderException;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.logging.Param;
import org.jboss.msc.service.StartException;

import java.io.InputStream;
import java.net.URL;

/**
 * This module is using message IDs in the range 15700-15999.
 * This file is using the subset 15800-10949 for server logger messages.
 * See http://community.jboss.org/docs/DOC-16810 for the full list of
 * currently reserved JBAS message id blocks.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Mike M. Clark
 */
@MessageBundle(projectCode = "JBAS")
public interface ServerMessages {

    /**
     * The messages
     */
    ServerMessages MESSAGES = Messages.getBundle(ServerMessages.class);

    /**
     * Creates an error message indicating a value was expected for the given command line option.
     *
     * @param option the name of the command line option
     *
     * @return a message that can by output to stderr.
     */
    @Message(id = 15800, value = "Value expected for option %s")
    String valueExpectedForCommandLineOption(String option);

    /**
     * Creates an error message indicating an invalid command line option was presented.
     *
     * @param option the name of the command line option
     *
     * @return a message that can by output to stderr.
     */
    @Message(id = 15801, value = "Invalid option '%s'")
    String invalidCommandLineOption(String option);

    /**
     * Creates an error message indicating a malformed URL was provided as a value for a command line option.
     *
     * @param urlSpec the provided url
     * @param option the name of the command line option
     *
     * @return a message that can by output to stderr.
     */
    @Message(id = 15802, value = "Malformed URL '%s' provided for option '%s'")
    String malformedCommandLineURL(String urlSpec, String option);

    /**
     * Creates an error message indicating {@link java.util.Properties#load(InputStream) properties could not be loaded}
     * from a given url.
     *
     * @param url the provided url
     *
     * @return a message that can by output to stderr.
     */
    @Message(id = 15803, value = "Unable to load properties from URL '%s'")
    String unableToLoadProperties(URL url);

    /**
     * Creates an error message indicating creating a security vault failed.
     *
     * @param cause the problem
     * @param msg the problem (for use in the message)
     *
     * @return a RuntimeException wrapper
     */
    @Message(id = 15804, value = "Error initializing vault --  %s")
    RuntimeException cannotCreateVault(@Param VaultReaderException cause, VaultReaderException msg);

    /**
     * Creates an error message indicating that connecting to the HC failed.
     *
     * @param e the problem
     * @return a StartException
     */
    @Message(id = 15805, value = "Failed to connect to the host-controller")
    StartException failedToConnectToHC(@Param Exception e);

    /**
     * Creates an error message indicating that the operation connecting to the
     * HC got cancelled before it could complete.
     *
     * @return a StartException
     */
    @Message(id = 15806, value = "Connection request to the host-controller was cancelled")
    StartException cancelledHCConnect();

    @Message(id = 15807, value = "hostControllerName must be null if the server is not in a managed domain")
    IllegalArgumentException hostControllerNameNonNullInStandalone();

    @Message(id = 15808, value = "hostControllerName may not be null if the server is in a managed domain")
    IllegalArgumentException hostControllerNameNullInDomain();

    @Message(id = 15809, value = "An IP address cannot be resolved using the given interface selection criteria. Failure was -- %s")
    OperationFailedException cannotResolveInterface(Exception msg, @Param Exception cause);
}

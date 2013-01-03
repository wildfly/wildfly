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

package org.jboss.as.network;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.Messages;

/**
 * Date: 24.06.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface NetworkMessages {
    /**
     * The default messages
     */
    NetworkMessages MESSAGES = Messages.getBundle(NetworkMessages.class);

    /**
     * Creates an exception indicating the value cannot be changed while the socket is bound.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 15300, value = "cannot change value while the socket is bound.")
    IllegalStateException cannotChangeWhileBound();

    /**
     * Creates an exception indicating the no multicast binding for the name.
     *
     * @param name the name.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 15301, value = "no multicast binding: %s")
    IllegalStateException noMulticastBinding(String name);

    /**
     * Creates an exception indicating parameter {@code name} cannot be null or an empty string.
     *
     * @param name the name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 15302, value = "%s cannot be null or an empty string")
    IllegalArgumentException nullOrEmptyVar(String name);

    /**
     * Creates an exception indicating a param of type {@code type} was null for the outbound socket binding {@code name}.
     *
     * @param type the type of the param
     * @param name the name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 15303, value = "%s cannot be null for outbound socket binding '%s'")
    IllegalArgumentException nullOutboundSocketBindingParam(String type, String name);

    /**
     * Creates an exception indicating no destination address for the outbound socket binding.
     *
     * @param name the name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 15304, value = "Destination address cannot be null or empty for outbound socket binding '%s'")
    IllegalArgumentException nullDestinationAddress(String name);

    /**
     * Creates an exception indicating a negative destination port for the outbound socket binding {@code name}.
     *
     * @param port the illegal port value
     * @param name the name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 15305, value = "Destination port cannot be a negative value: [%d] for outbound socket binding '%s'")
    IllegalArgumentException negativeDestinationPort(int port, String name);
}

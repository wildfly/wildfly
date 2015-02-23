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
package org.wildfly.clustering.jgroups.spi;

import java.nio.ByteBuffer;

/**
 * Factory for creating JGroups channels.
 * @author Paul Ferraro
 */
public interface ChannelFactory extends org.wildfly.clustering.jgroups.ChannelFactory {

    /**
     * Returns the protocol stack configuration of this channel factory.
     * @return the protocol stack configuration of this channel factory
     */
    ProtocolStackConfiguration getProtocolStackConfiguration();

    /**
     * Determines whether or not the specified message response indicates the fork stack or fork channel
     * required to handle a request does not exist on the recipient node.
     * @param response a message response
     * @return true, if the response indicates a missing fork stack or channel.
     */
    boolean isUnknownForkResponse(ByteBuffer response);
}

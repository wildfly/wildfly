/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.jgroups.spi.service;

import org.jboss.msc.service.ServiceName;

/**
 * Factory for creating service names for channel-based services
 * @author Paul Ferraro
 */
public interface ChannelServiceNameFactory {

    /**
     * The alias for the default channel.
     */
    String DEFAULT_CHANNEL = "default";

    /**
     * Returns an appropriate service name for the default channel
     * @return
     */
    ServiceName getServiceName();

    /**
     * Returns an appropriate service name for the specified channel
     * @param name the channel name
     * @return
     */
    ServiceName getServiceName(String name);
}

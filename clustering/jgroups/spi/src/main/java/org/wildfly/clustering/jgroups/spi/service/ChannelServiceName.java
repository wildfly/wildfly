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

public enum ChannelServiceName implements ChannelServiceNameFactory {

    CHANNEL {
        @Override
        public ServiceName getServiceName(String channel) {
            return ProtocolStackServiceName.BASE_NAME.append("channel", channel);
        }
    },
    FACTORY {
        @Override
        public ServiceName getServiceName(String channel) {
            return CHANNEL.getServiceName(channel).append("factory");
        }
    },
    CONNECTOR {
        @Override
        public ServiceName getServiceName(String channel) {
            return CHANNEL.getServiceName(channel).append("connector");
        }
    },
    ;

    @Override
    public ServiceName getServiceName() {
        return this.getServiceName(DEFAULT_CHANNEL);
    }
}

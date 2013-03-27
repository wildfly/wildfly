/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.host.controller.mgmt;

import org.jboss.as.controller.transform.Transformers;
import org.jboss.remoting3.Attachments;
import org.jboss.remoting3.Channel;

/**
 * Manages attachments on the domain controller for each slave host controller channel.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class SlaveChannelAttachments {

    private static final Attachments.Key<HostChannelInfo> HOST_CHANNEL_INFO = new Attachments.Key<HostChannelInfo>(HostChannelInfo.class);

    static void attachSlaveInfo(Channel channel, String hostName, Transformers transformers) {
        channel.getAttachments().attach(HOST_CHANNEL_INFO, new HostChannelInfo(hostName, transformers));
    }

    static String getHostName(Channel channel) {
        return channel.getAttachments().getAttachment(HOST_CHANNEL_INFO).hostName;
    }

    static Transformers getTransformers(Channel channel) {
        return channel.getAttachments().getAttachment(HOST_CHANNEL_INFO).transformers;
    }

    private static class HostChannelInfo {
        final String hostName;
        final Transformers transformers;

        public HostChannelInfo(String hostName, Transformers transformers) {
            this.hostName = hostName;
            this.transformers = transformers;
        }
    }

}

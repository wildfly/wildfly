/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.jgroups;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.fork.UnknownForkHandler;
import org.jgroups.protocols.FORK;
import org.jgroups.stack.Protocol;
import org.jgroups.util.MessageBatch;
import org.jgroups.util.Util;

/**
 * Workaround for JGRP-2294. Overrides ForkProtocolStack.up(MessageBatch) to trigger unknown fork handler when necessary
 * @author Paul Ferraro
 */
public class ForkProtocolStack extends org.jgroups.fork.ForkProtocolStack {

    public ForkProtocolStack(UnknownForkHandler unknownForkHandler, List<Protocol> protocols, String fork_stack_id) {
        super(unknownForkHandler, protocols, fork_stack_id);
    }

    @Override
    public void up(MessageBatch batch) {
        // Sort fork messages by fork-channel-id
        Map<String, List<Message>> forkMessages = new HashMap<>();
        for (Message message : batch) {
            FORK.ForkHeader header = message.getHeader(FORK.ID);
            if (header != null) {
                batch.remove(message);
                String forkChannelId = header.getForkChannelId();
                List<Message> messages = forkMessages.get(forkChannelId);
                if (messages == null) {
                    messages = new ArrayList<>(batch.size());
                    forkMessages.put(forkChannelId, messages);
                }
                messages.add(message);
            }
        }

        // Now pass fork messages up, batched by fork-channel-id
        for (Map.Entry<String, List<Message>> entry : forkMessages.entrySet()) {
            String forkChannelId = entry.getKey();
            List<Message> messages = entry.getValue();
            JChannel forkChannel = this.get(forkChannelId);
            if (forkChannel == null) {
                for (Message message : messages) {
                    this.unknownForkHandler.handleUnknownForkChannel(message, forkChannelId);
                }
                continue;
            }
            MessageBatch forkBatch = new MessageBatch(batch.dest(), batch.sender(), batch.clusterName(), batch.multicast(), messages);
            try {
                forkChannel.up(forkBatch);
            }
            catch (Throwable t) {
                this.log.error(Util.getMessage("FailedPassingUpBatch"), t);
            }
        }

        if (!batch.isEmpty()) {
            this.up_prot.up(batch);
        }
    }
}

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

import org.jgroups.Message;
import org.jgroups.fork.ForkProtocol;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolStack;
import org.jgroups.util.MessageBatch;
import org.jgroups.util.Util;

/**
 * Workaround for JGRP-2294. Overrides Fork.up(MessageBatch) to trigger unknown fork handler when necessary
 * @author Paul Ferraro
 */
public class FORK extends org.jgroups.protocols.FORK {
    @Override
    public void up(MessageBatch batch) {
        // Sort fork messages by fork-stack-id
        Map<String, List<Message>> forkMessages = new HashMap<>();
        for (Message message : batch) {
            ForkHeader header = message.getHeader(this.id);
            if (header != null) {
                batch.remove(message);
                String forkStackId = header.getForkStackId();
                List<Message> messages = forkMessages.get(forkStackId);
                if (messages == null) {
                    messages = new ArrayList<>(batch.size());
                    forkMessages.put(forkStackId, messages);
                }
                messages.add(message);
            }
        }

        // Now pass fork messages up, batched by fork-stack-id
        for (Map.Entry<String, List<Message>> entry : forkMessages.entrySet()) {
            String forkStackId = entry.getKey();
            List<Message> messages = entry.getValue();
            Protocol bottom = this.get(forkStackId);
            if (bottom == null) {
                for (Message msg : messages) {
                    this.getUnknownForkHandler().handleUnknownForkStack(msg, forkStackId);
                }
                continue;
            }
            MessageBatch forkBatch = new MessageBatch(batch.dest(), batch.sender(), batch.clusterName(), batch.multicast(), messages);
            try {
                bottom.up(forkBatch);
            }
            catch (Throwable t) {
                this.log.error(Util.getMessage("FailedPassingUpBatch"), t);
            }
        }

        if (!batch.isEmpty()) {
            this.up_prot.up(batch);
        }
    }

    @Override
    public synchronized ProtocolStack createForkStack(String forkStackId, List<Protocol> protocols, boolean initialize) throws Exception {
        Protocol bottom = this.get(forkStackId);
        if (bottom != null) {
            org.jgroups.fork.ForkProtocolStack stack = getForkStack(bottom);
            return initialize ? stack.incrInits() : stack;
        }

        List<Protocol> forkProtocols = new ArrayList<>((protocols != null) ? protocols.size() + 1 : 1);
        bottom = new ForkProtocol(forkStackId).setDownProtocol(this);
        forkProtocols.add(bottom); // add a ForkProtocol as bottom protocol
        if (protocols != null) {
            forkProtocols.addAll(protocols);
        }
        ForkProtocolStack forkStack = new ForkProtocolStack(this.getUnknownForkHandler(), forkProtocols, forkStackId);
        forkStack.setChannel(this.stack.getChannel());
        forkStack.init();
        if (initialize) {
            forkStack.incrInits();
        }
        this.fork_stacks.put(forkStackId, bottom);
        return forkStack;
    }
}

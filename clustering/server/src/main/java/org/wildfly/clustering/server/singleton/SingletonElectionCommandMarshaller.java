/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.server.singleton;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.clustering.server.group.AddressableNode;
import org.wildfly.clustering.server.group.LocalNode;

/**
 * @author Paul Ferraro
 */
public class SingletonElectionCommandMarshaller implements ProtoStreamMarshaller<SingletonElectionCommand> {

    private static final int CANDIDATE_INDEX = 1;
    private static final int LOCAL_CANDIDATE_INDEX = 2;
    private static final int ELECTED_INDEX = 3;

    @Override
    public SingletonElectionCommand readFrom(ProtoStreamReader reader) throws IOException {
        List<Node> candidates = new LinkedList<>();
        Integer elected = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case CANDIDATE_INDEX:
                    candidates.add(reader.readObject(AddressableNode.class));
                    break;
                case LOCAL_CANDIDATE_INDEX:
                    candidates.add(reader.readObject(LocalNode.class));
                    break;
                case ELECTED_INDEX:
                    elected = Integer.valueOf(reader.readUInt32());
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new SingletonElectionCommand(candidates, elected);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, SingletonElectionCommand command) throws IOException {
        for (Node candidate : command.getCandidates()) {
            writer.writeObject((candidate instanceof LocalNode) ? LOCAL_CANDIDATE_INDEX : CANDIDATE_INDEX, candidate);
        }
        Integer elected = command.getIndex();
        if (elected != null) {
            writer.writeUInt32(ELECTED_INDEX, elected.intValue());
        }
    }

    @Override
    public Class<? extends SingletonElectionCommand> getJavaClass() {
        return SingletonElectionCommand.class;
    }
}

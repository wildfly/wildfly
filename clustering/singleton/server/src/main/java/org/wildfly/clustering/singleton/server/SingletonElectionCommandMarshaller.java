/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.clustering.server.GroupMember;

/**
 * @author Paul Ferraro
 */
public class SingletonElectionCommandMarshaller implements ProtoStreamMarshaller<SingletonElectionCommand> {

    private static final int CANDIDATE_INDEX = 1;
    private static final int ELECTED_INDEX = 2;

    @Override
    public SingletonElectionCommand readFrom(ProtoStreamReader reader) throws IOException {
        List<GroupMember> candidates = new LinkedList<>();
        Integer elected = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case CANDIDATE_INDEX:
                    candidates.add(reader.readAny(GroupMember.class));
                    break;
                case ELECTED_INDEX:
                    elected = reader.readUInt32();
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new SingletonElectionCommand(candidates, elected);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, SingletonElectionCommand command) throws IOException {
        for (GroupMember candidate : command.getCandidates()) {
            writer.writeAny(CANDIDATE_INDEX, candidate);
        }
        Integer elected = command.getIndex();
        if (elected != null) {
            writer.writeUInt32(ELECTED_INDEX, elected);
        }
    }

    @Override
    public Class<? extends SingletonElectionCommand> getJavaClass() {
        return SingletonElectionCommand.class;
    }
}

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
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;

/**
 * @author Paul Ferraro
 */
public class SingletonElectionCommandExternalizer implements Externalizer<SingletonElectionCommand> {

    @Override
    public void writeObject(ObjectOutput output, SingletonElectionCommand command) throws IOException {
        List<Node> candidates = command.getCandidates();
        Integer index = command.getIndex();
        IndexSerializer.VARIABLE.writeInt(output, candidates.size());
        for (Node candidate : candidates) {
            output.writeObject(candidate);
        }
        IndexSerializer.select(candidates.size() + 1).writeInt(output, (index != null) ? index : candidates.size());
    }

    @Override
    public SingletonElectionCommand readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        int size = IndexSerializer.VARIABLE.readInt(input);
        List<Node> candidates = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            candidates.add((Node) input.readObject());
        }
        int index = IndexSerializer.select(size + 1).readInt(input);
        return new SingletonElectionCommand(candidates, (index != size) ? Integer.valueOf(index) : null);
    }

    @Override
    public Class<SingletonElectionCommand> getTargetClass() {
        return SingletonElectionCommand.class;
    }
}

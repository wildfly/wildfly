/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.util.List;

import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.dispatcher.Command;
import org.wildfly.clustering.singleton.election.SingletonElectionListener;

/**
 * @author Paul Ferraro
 */
public class SingletonElectionCommand implements Command<Void, SingletonElectionListener, RuntimeException> {

    private final List<GroupMember> candidates;
    private final Integer index;

    public SingletonElectionCommand(List<GroupMember> candidates, GroupMember elected) {
        this(candidates, (elected != null) ? candidates.indexOf(elected) : null);
    }

    SingletonElectionCommand(List<GroupMember> candidates, Integer index) {
        this.candidates = candidates;
        this.index = index;
    }

    List<GroupMember> getCandidates() {
        return this.candidates;
    }

    Integer getIndex() {
        return this.index;
    }

    @Override
    public Void execute(SingletonElectionListener context) {
        context.elected(this.candidates, (this.index != null) ? this.candidates.get(this.index) : null);
        return null;
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.util.List;

import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.singleton.SingletonElectionListener;

/**
 * @author Paul Ferraro
 */
public class SingletonElectionCommand implements Command<Void, SingletonElectionListener> {
    private static final long serialVersionUID = 8457549139382922406L;

    private final List<Node> candidates;
    private final Integer index;

    public SingletonElectionCommand(List<Node> candidates, Node elected) {
        this(candidates, (elected != null) ? candidates.indexOf(elected) : null);
    }

    SingletonElectionCommand(List<Node> candidates, Integer index) {
        this.candidates = candidates;
        this.index = index;
    }

    List<Node> getCandidates() {
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

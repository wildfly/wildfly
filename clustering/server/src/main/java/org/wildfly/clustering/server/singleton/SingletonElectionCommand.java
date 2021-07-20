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

package org.wildfly.clustering.server.singleton;

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

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.singleton.service;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.singleton.SingletonElectionListener;

/**
 * @author Paul Ferraro
 */
public class SingletonElectionListenerService implements Service, SingletonElectionListener, Supplier<Node> {
    private final Consumer<Supplier<Node>> injector;
    private volatile Node primaryMember = null;

    public SingletonElectionListenerService(Consumer<Supplier<Node>> injector) {
        this.injector = injector;
    }

    @Override
    public Node get() {
        return this.primaryMember;
    }

    @Override
    public void start(StartContext context) throws StartException {
        this.injector.accept(this);
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public void elected(List<Node> candidateMembers, Node electedMember) {
        this.primaryMember = electedMember;
    }
}

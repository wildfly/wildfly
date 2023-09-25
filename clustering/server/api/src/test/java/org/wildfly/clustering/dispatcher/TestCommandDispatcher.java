/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.dispatcher;

import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.wildfly.clustering.group.Node;

/**
 * {@link CommandDispatcher} that delegates to a mock dispatcher.
 * @author Paul Ferraro
 */
public class TestCommandDispatcher<C> implements CommandDispatcher<C> {

    private final CommandDispatcher<C> dispatcher;

    public TestCommandDispatcher(CommandDispatcher<C> dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public C getContext() {
        return this.dispatcher.getContext();
    }

    @Override
    public <R> CompletionStage<R> executeOnMember(Command<R, ? super C> command, Node member) throws CommandDispatcherException {
        return this.dispatcher.executeOnMember(command, member);
    }

    @Override
    public <R> Map<Node, CompletionStage<R>> executeOnGroup(Command<R, ? super C> command, Node... excludedMembers) throws CommandDispatcherException {
        return this.dispatcher.executeOnGroup(command, excludedMembers);
    }

    @Override
    public void close() {
        this.dispatcher.close();
    }
}

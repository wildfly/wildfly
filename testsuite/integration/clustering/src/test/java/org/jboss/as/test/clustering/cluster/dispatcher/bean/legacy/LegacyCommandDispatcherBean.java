/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.dispatcher.bean.legacy;

import java.util.Map;
import java.util.concurrent.CompletionStage;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.EJB;
import jakarta.ejb.Local;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.Node;

@Singleton
@Startup
@Local(CommandDispatcher.class)
public class LegacyCommandDispatcherBean implements CommandDispatcher<Node> {
    @EJB
    private CommandDispatcherFactory factory;
    private CommandDispatcher<Node> dispatcher;

    @PostConstruct
    public void init() {
        this.dispatcher = this.factory.createCommandDispatcher(this.getClass().getSimpleName(), this.getContext());
    }

    @PreDestroy
    public void destroy() {
        this.close();
    }

    @Override
    public <R> CompletionStage<R> executeOnMember(Command<R, ? super Node> command, Node member) throws CommandDispatcherException {
        return this.dispatcher.executeOnMember(command, member);
    }

    @Override
    public <R> Map<Node, CompletionStage<R>> executeOnGroup(Command<R, ? super Node> command, Node... excludedMembers) throws CommandDispatcherException {
        return this.dispatcher.executeOnGroup(command, excludedMembers);
    }

    @Override
    public void close() {
        this.dispatcher.close();
    }

    @Override
    public Node getContext() {
        return this.factory.getGroup().getLocalMember();
    }
}

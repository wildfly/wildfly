/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.dispatcher.bean;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.EJB;
import jakarta.ejb.Local;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import org.wildfly.clustering.server.dispatcher.Command;
import org.wildfly.clustering.server.dispatcher.CommandDispatcher;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.GroupMember;

@Singleton
@Startup
@Local(CommandDispatcher.class)
public class CommandDispatcherBean implements CommandDispatcher<GroupMember, GroupMember> {
    @EJB
    private CommandDispatcherFactory<GroupMember> factory;
    private CommandDispatcher<GroupMember, GroupMember> dispatcher;

    @PostConstruct
    public void init() {
        this.dispatcher = this.factory.createCommandDispatcher(this.getClass().getSimpleName(), this.getContext());
    }

    @PreDestroy
    public void destroy() {
        this.close();
    }

    @Override
    public <R, E extends Exception> CompletionStage<R> dispatchToMember(Command<R, ? super GroupMember, E> command, GroupMember member) throws IOException {
        return this.dispatcher.dispatchToMember(command, member);
    }

    @Override
    public <R, E extends Exception> Map<GroupMember, CompletionStage<R>> dispatchToGroup(Command<R, ? super GroupMember, E> command, Set<GroupMember> excluding) throws IOException {
        return this.dispatcher.dispatchToGroup(command, excluding);
    }

    @Override
    public void close() {
        this.dispatcher.close();
    }

    @Override
    public GroupMember getContext() {
        return this.factory.getGroup().getLocalMember();
    }
}

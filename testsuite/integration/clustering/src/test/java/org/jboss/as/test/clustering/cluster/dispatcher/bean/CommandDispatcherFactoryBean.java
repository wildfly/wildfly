/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.dispatcher.bean;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.Local;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.wildfly.clustering.server.Registration;
import org.wildfly.clustering.server.dispatcher.CommandDispatcher;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.Group;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.GroupMembershipEvent;
import org.wildfly.clustering.server.GroupMembershipListener;

@Singleton
@Startup
@Local(CommandDispatcherFactory.class)
public class CommandDispatcherFactoryBean implements CommandDispatcherFactory<GroupMember>, GroupMembershipListener<GroupMember> {

    @Resource(name = "clustering/command-dispatcher-factory")
    private CommandDispatcherFactory<GroupMember> factory;
    private Registration registration;

    @PostConstruct
    public void init() {
        this.registration = this.factory.getGroup().register(this);
    }

    @PreDestroy
    public void destroy() {
        this.registration.close();
    }

    @Override
    public <C> CommandDispatcher<GroupMember, C> createCommandDispatcher(Object service, C context, ClassLoader loader) {
        return this.factory.createCommandDispatcher(service, context, loader);
    }

    @Override
    public Group<GroupMember> getGroup() {
        return this.factory.getGroup();
    }

    @Override
    public void updated(GroupMembershipEvent<GroupMember> event) {
        try {
            // Ensure the thread context classloader of the notification is correct
            Thread.currentThread().getContextClassLoader().loadClass(this.getClass().getName());
            // Ensure the correct naming context is set
            Context context = new InitialContext();
            try {
                context.lookup("java:comp/env/clustering/command-dispatcher-factory");
            } finally {
                context.close();
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (NamingException e) {
            throw new IllegalStateException(e);
        }
        System.out.println(String.format("Previous membership = %s, current membership = %s", event.getPreviousMembership(), event.getCurrentMembership()));
    }
}

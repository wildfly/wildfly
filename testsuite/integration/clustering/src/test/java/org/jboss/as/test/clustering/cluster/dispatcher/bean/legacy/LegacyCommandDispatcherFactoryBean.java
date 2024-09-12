/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.dispatcher.bean.legacy;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.Local;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.wildfly.clustering.Registration;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.GroupListener;
import org.wildfly.clustering.group.Membership;

@Singleton
@Startup
@Local(CommandDispatcherFactory.class)
public class LegacyCommandDispatcherFactoryBean implements CommandDispatcherFactory, GroupListener {

    @Resource(name = "clustering/dispatcher")
    private CommandDispatcherFactory factory;
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
    public <C> CommandDispatcher<C> createCommandDispatcher(Object service, C context) {
        return this.factory.createCommandDispatcher(service, context);
    }

    @Override
    public Group getGroup() {
        return this.factory.getGroup();
    }

    @Override
    public void membershipChanged(Membership previousMembership, Membership membership, boolean merged) {
        try {
            // Ensure the thread context classloader of the notification is correct
            Thread.currentThread().getContextClassLoader().loadClass(this.getClass().getName());
            // Ensure the correct naming context is set
            Context context = new InitialContext();
            try {
                context.lookup("java:comp/env/clustering/dispatcher");
            } finally {
                context.close();
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (NamingException e) {
            throw new IllegalStateException(e);
        }
        System.out.println(String.format("Previous membership = %s, current membership = %s, merged = %s", previousMembership, membership, merged));
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.group.bean;

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
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.GroupMembership;
import org.wildfly.clustering.server.GroupMembershipEvent;
import org.wildfly.clustering.server.GroupMembershipListener;

@Singleton
@Startup
@Local(Group.class)
public class GroupBean implements Group, GroupMembershipListener<GroupMember> {

    @Resource(name = "clustering/server/group")
    private org.wildfly.clustering.server.Group<GroupMember> group;
    private Registration registration;
    private volatile GroupMembership<GroupMember> previousMembership;

    @PostConstruct
    public void init() {
        this.registration = this.group.register(this);
    }

    @PreDestroy
    public void destroy() {
        this.registration.close();
    }

    @Override
    public void updated(GroupMembershipEvent<GroupMember> event) {
        try {
            // Ensure the thread context classloader of the notification is correct
            Thread.currentThread().getContextClassLoader().loadClass(this.getClass().getName());
            // Ensure the correct naming context is set
            Context context = new InitialContext();
            try {
                context.lookup("java:comp/env/clustering/group");
            } finally {
                context.close();
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (NamingException e) {
            throw new IllegalStateException(e);
        }
        this.previousMembership = event.getPreviousMembership();
    }

    @Override
    public String getName() {
        return this.group.getName();
    }

    @Override
    public GroupMember getLocalMember() {
        return this.group.getLocalMember();
    }

    @Override
    public GroupMembership<GroupMember> getMembership() {
        return this.group.getMembership();
    }

    @Override
    public boolean isSingleton() {
        return this.group.isSingleton();
    }

    @Override
    public Registration register(GroupMembershipListener<GroupMember> object) {
        return this.group.register(object);
    }

    @Override
    public GroupMembership<GroupMember> getPreviousMembership() {
        return this.previousMembership;
    }
}

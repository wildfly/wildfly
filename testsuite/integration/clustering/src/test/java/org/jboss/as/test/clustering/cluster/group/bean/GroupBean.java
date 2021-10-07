package org.jboss.as.test.clustering.cluster.group.bean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.wildfly.clustering.Registration;
import org.wildfly.clustering.group.GroupListener;
import org.wildfly.clustering.group.Membership;
import org.wildfly.clustering.group.Node;

@Singleton
@Startup
@Local(Group.class)
public class GroupBean implements Group, GroupListener {

    @Resource(name = "clustering/group")
    private org.wildfly.clustering.group.Group group;
    private Registration registration;
    private volatile Membership previousMembership;

    @PostConstruct
    public void init() {
        this.registration = this.group.register(this);
    }

    @PreDestroy
    public void destroy() {
        this.registration.close();
    }

    @Override
    public void membershipChanged(Membership previousMembership, Membership membership, boolean merged) {
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
        this.previousMembership = previousMembership;
    }

    @Override
    public String getName() {
        return this.group.getName();
    }

    @Override
    public Node getLocalMember() {
        return this.group.getLocalMember();
    }

    @Override
    public Membership getMembership() {
        return this.group.getMembership();
    }

    @Override
    public boolean isSingleton() {
        return this.group.isSingleton();
    }

    @Override
    public Registration register(GroupListener object) {
        return this.group.register(object);
    }

    @Override
    public Membership getPreviousMembership() {
        return this.previousMembership;
    }
}

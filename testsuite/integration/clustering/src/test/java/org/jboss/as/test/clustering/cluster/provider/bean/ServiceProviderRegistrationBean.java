/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.provider.bean;

import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.Local;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.logging.Logger;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.provider.ServiceProviderListener;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrar;
import org.wildfly.clustering.server.provider.ServiceProviderRegistration;

@Singleton
@Startup
@Local(ServiceProviderRegistration.class)
public class ServiceProviderRegistrationBean implements ServiceProviderRegistration<String, GroupMember>, ServiceProviderListener<GroupMember> {
    static final Logger log = Logger.getLogger(ServiceProviderRegistrationBean.class);

    @Resource(name = "clustering/service-provider-registrar")
    private ServiceProviderRegistrar<String, GroupMember> factory;
    private ServiceProviderRegistration<String, GroupMember> registration;

    @PostConstruct
    public void init() {
        this.registration = this.factory.register(this.getClass().getSimpleName(), this);
    }

    @PreDestroy
    public void destroy() {
        this.close();
    }

    @Override
    public String getService() {
        return this.registration.getService();
    }

    @Override
    public Set<GroupMember> getProviders() {
        return this.registration.getProviders();
    }

    @Override
    public void close() {
        this.registration.close();
    }

    @Override
    public void providersChanged(Set<GroupMember> providers) {
        try {
            // Ensure the thread context classloader of the notification is correct
            Thread.currentThread().getContextClassLoader().loadClass(this.getClass().getName());
            // Ensure the correct naming context is set
            Context context = new InitialContext();
            try {
                context.lookup("java:comp/env/clustering/service-provider-registrar");
            } finally {
                context.close();
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (NamingException e) {
            throw new IllegalStateException(e);
        }
        log.info(String.format("ServiceProviderListener.providersChanged(%s)", providers));
    }
}

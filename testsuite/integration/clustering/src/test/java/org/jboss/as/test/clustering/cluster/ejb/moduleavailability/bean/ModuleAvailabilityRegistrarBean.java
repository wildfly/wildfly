/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.moduleavailability.bean;

import java.util.Set;

import jakarta.annotation.Resource;
import jakarta.ejb.Local;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import org.wildfly.clustering.server.Group;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrar;
import org.wildfly.clustering.server.provider.ServiceProviderRegistration;
import org.wildfly.clustering.server.provider.ServiceProviderListener;

/**
 * An EJB wrapper for the ServiceProviderRegistrar instance used by the ModuleAvailabilityListenerService which manages
 * module availability updates to EJB clients.
 */
@Singleton
@Startup
@Local(ServiceProviderRegistrar.class)
public class ModuleAvailabilityRegistrarBean implements ServiceProviderRegistrar<Object, GroupMember>, ServiceProviderListener<GroupMember> {

    // inject the ServiceProviderRegistrar instance used by ModuleAvailabilityRegistrarService
    @Resource(lookup="java:jboss/clustering/server/service-provider-registrar/ejb/client-services")
    ServiceProviderRegistrar<Object, GroupMember> registrar;

    // ServiceProviderRegistrar interface
    @Override
    public Group<GroupMember> getGroup() {
        return registrar.getGroup();
    }

    @Override
    public ServiceProviderRegistration<Object, GroupMember> register(Object service) {
        return registrar.register(service);
    }

    @Override
    public ServiceProviderRegistration<Object, GroupMember> register(Object service, ServiceProviderListener<GroupMember> listener) {
        return registrar.register(service, listener);
    }

    @Override
    public Set<GroupMember> getProviders(Object service) {
        return registrar.getProviders(service);
    }

    @Override
    public Set<Object> getServices() {
        return registrar.getServices();
    }

    // ServiceProviderRegistrarListener interface
    @Override
    public void providersChanged(Set<GroupMember> providers) {
        System.out.println("Providers changed: " + providers);
    }

}

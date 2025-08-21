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

import org.jboss.logging.Logger;
import org.wildfly.clustering.server.Group;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrar;
import org.wildfly.clustering.server.provider.ServiceProviderRegistration;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrationEvent;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrationListener;

/**
 * An EJB wrapper for the ServiceProviderRegistrar instance used by the ModuleAvailabilityListenerService which manages
 * module availability updates to EJB clients.
 */
@Singleton
@Startup
@Local(ServiceProviderRegistrar.class)
public class ModuleAvailabilityRegistrarBean implements ServiceProviderRegistrar<Object, GroupMember>, ServiceProviderRegistrationListener<GroupMember> {

    protected static final Logger LOGGER = Logger.getLogger(ModuleAvailabilityRegistrarBean.class.getSimpleName());

    // inject the ServiceProviderRegistrar instance used by ModuleAvailabilityRegistrarService
    @Resource(lookup="java:jboss/clustering/server/service-provider-registrar/ejb/client-services")
    ServiceProviderRegistrar<Object, GroupMember> registrar;

    // ServiceProviderRegistrar interface
    @Override
    public Group<GroupMember> getGroup() {
        LOGGER.info("Calling getGroup()");
        return registrar.getGroup();
    }

    @Override
    public ServiceProviderRegistration<Object, GroupMember> register(Object service) {
        LOGGER.infof("Calling register with identifier %s\n", service);
        return registrar.register(service);
    }

    @Override
    public ServiceProviderRegistration<Object, GroupMember> register(Object service, ServiceProviderRegistrationListener<GroupMember> listener) {
        LOGGER.infof("Calling register with identifier %s and listener %s\n", service, listener);
        return registrar.register(service, listener);
    }

    @Override
    public Set<Object> getServices() {
        LOGGER.info("Calling getServices()");
        return registrar.getServices();
    }

    @Override
    public Set<GroupMember> getProviders(Object service) {
        LOGGER.infof("Calling getProviders() with identifier %s\n", service);
        return registrar.getProviders(service);
    }

    // ServiceProviderRegistrarListener interface
    @Override
    public void providersChanged(ServiceProviderRegistrationEvent<GroupMember> event) {
        LOGGER.infof("Providers changed: previous %s, current %s", event.getPreviousProviders(), event.getCurrentProviders());
    }

}

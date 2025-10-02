/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.moduleavailability.bean;

import jakarta.annotation.Resource;
import jakarta.ejb.Local;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.jboss.logging.Logger;
import org.wildfly.clustering.server.Group;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.provider.ServiceProviderListener;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrar;
import org.wildfly.clustering.server.provider.ServiceProviderRegistration;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrationListener;

import java.util.Set;

/**
 * An EJB wrapper for the ServiceProviderRegistrar instance used by the ModuleAvailabilityListenerService which manages
 * module availability updates to EJB clients.
 */
@Singleton
@Startup
@Local(ServiceProviderRegistrar.class)
public class ModuleAvailabilityRegistrarBean implements ServiceProviderRegistrar<Object, GroupMember>, ServiceProviderListener<GroupMember> {

    protected static final Logger log = Logger.getLogger(ModuleAvailabilityRegistrarBean.class.getSimpleName());

    // inject the ServiceProviderRegistrar instance used by ModuleAvailabilityRegistrarService
    @Resource(lookup="java:jboss/clustering/server/service-provider-registrar/ejb/client-services")
    ServiceProviderRegistrar<Object, GroupMember> registrar;

    // ServiceProviderRegistrar interface
    @Override
    public Group<GroupMember> getGroup() {
        log.info("Calling getGroup()");
        return registrar.getGroup();
    }

    @Override
    public ServiceProviderRegistration<Object, GroupMember> register(Object service) {
        log.infof("Calling register() with identifier %s\n", service);
        return registrar.register(service);
    }

    @Override
    public ServiceProviderRegistration<Object, GroupMember> register(Object service, ServiceProviderRegistrationListener<GroupMember> listener) {
        // noop
        return registrar.register(service, listener);
    }

    @Override
    public Set<GroupMember> getProviders(Object service) {
        log.infof("Calling getProviders() with identifier %s\n", service);
        Set<GroupMember> result = registrar.getProviders(service);
        log.infof("Called getProviders() with identifier %s: result = %s\n", service, result);
        return result;
//        return registrar.getProviders(service);
    }

    @Override
    public Set<Object> getServices() {
        log.info("Calling getServices()");
        Set<Object> result = registrar.getServices();
        log.infof("Called getServices(): result = %s", result);
        return result;
//        return registrar.getServices();
    }

    // ServiceProviderRegistrarListener interface
    @Override
    public void providersChanged(Set<GroupMember> providers) {
        log.infof("Calling providers changed: provider set %s", providers);
    }
}

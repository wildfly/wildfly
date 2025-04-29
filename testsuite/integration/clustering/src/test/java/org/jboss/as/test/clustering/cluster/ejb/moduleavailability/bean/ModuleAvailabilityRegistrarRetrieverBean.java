/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.moduleavailability.bean;

import java.util.Set;
import java.util.HashSet;

import jakarta.ejb.EJB;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

import org.jboss.ejb.client.EJBModuleIdentifier;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrar;

/**
 * A stateless bean which provides remote access to the ServiceProviderRegistrar instance of the ModuleAvaiabilityRegistrar
 * service.
 *
 * @author Richard Achmatowicz
 */
@Stateless
@Remote(ModuleAvailabilityRegistrarRetriever.class)
public class ModuleAvailabilityRegistrarRetrieverBean implements ModuleAvailabilityRegistrarRetriever {

    @EJB
    ServiceProviderRegistrar<Object, GroupMember> registrar;

    /**
     * Obtains the services currently provided by the ServiceProviderRegistrar and returns the set of service names.
     * @return the service names
     */
    @Override
    public Set<String> getServices() {
        Set<String> deployedModules = new HashSet();
        System.out.println("Calling getServices()");
        for (Object module : registrar.getServices()) {
            // cast each registered service to EJBModuleIdentifier
            String moduleId = ((EJBModuleIdentifier) module).toString();
            deployedModules.add(moduleId);
        }
        return deployedModules;
    }

    /**
     * Obtains the current set of providers (nodes) for a given service.
     * @param service
     * @return set of nodes providing the service
     */
    @Override
    public Set<String> getProviders(Object service) {
        Set<String> providers = new HashSet();
        System.out.println("Calling getProviders(" + ((EJBModuleIdentifier) service)  + ")");
        for (GroupMember member : registrar.getProviders(service)) {
            providers.add(member.getName());
        }
        return providers;
    }
}

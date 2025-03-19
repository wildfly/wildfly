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

import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrar;

/**
 * A stateless bean which provides remote access to the ServiceProviderRegistrar instance of the ModuleAvaiabilityRegistrar
 * service.
 */
@Stateless
@Remote(ModuleAvailabilityRegistrarRetriever.class)
public class ModuleAvailabilityRegistrarRetrieverBean implements ModuleAvailabilityRegistrarRetriever {

    @EJB
    ServiceProviderRegistrar<Object, GroupMember> registrar;

    @Override
    public Set<String> getServices() {
        Set<String> deployedModules = new HashSet();
        System.out.println("Calling getServices()");
        for (Object module : registrar.getServices()) {
            // cast each registered service to DeploymentModuleIdentifier
            String moduleId = ((DeploymentModuleIdentifier) module).toString();
            deployedModules.add(moduleId);
        }
        return deployedModules;
    }
}
